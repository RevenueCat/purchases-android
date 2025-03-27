package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocaleList
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.currentlySubscribed
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptySet
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import java.util.Date
import android.os.LocaleList as FrameworkLocaleList

internal sealed interface PaywallState {
    object Loading : PaywallState

    data class Error(val errorMessage: String) : PaywallState {
        init {
            Logger.e("Paywall transitioned to error state: $errorMessage")
        }
    }

    sealed interface Loaded : PaywallState {
        val offering: Offering

        data class Legacy(
            override val offering: Offering,
            val templateConfiguration: TemplateConfiguration,
            val selectedPackage: MutableState<TemplateConfiguration.PackageInfo>,
            val shouldDisplayDismissButton: Boolean,
        ) : Loaded {

            constructor(
                offering: Offering,
                templateConfiguration: TemplateConfiguration,
                selectedPackage: TemplateConfiguration.PackageInfo,
                shouldDisplayDismissButton: Boolean,
            ) :
                this(
                    offering,
                    templateConfiguration,
                    mutableStateOf(selectedPackage),
                    shouldDisplayDismissButton,
                )

            fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
                selectedPackage.value = packageInfo
            }
        }

        @Suppress("LongParameterList")
        @Stable
        class Components(
            val stack: ComponentStyle,
            val stickyFooter: ComponentStyle?,
            val background: BackgroundStyles,
            /**
             * Some currencies do not commonly use decimals when displaying prices. Set this to false to accommodate
             * for that.
             */
            val showPricesWithDecimals: Boolean,
            val variableConfig: VariableConfig,
            override val offering: Offering,
            /**
             * All locales that this paywall supports, with `locales.head` being the default one.
             */
            private val locales: NonEmptySet<LocaleId>,
            private val activelySubscribedProductIds: Set<String>,
            private val purchasedNonSubscriptionProductIds: Set<String>,
            private val dateProvider: () -> Date,
            private val packages: AvailablePackages,
            initialLocaleList: LocaleList = LocaleList(
                (AppCompatDelegate.getApplicationLocales().toComposeLocaleList() + LocaleList.current).distinct(),
            ),
            initialSelectedTabIndex: Int? = null,
        ) : Loaded {

            data class AvailablePackages(
                val packagesOutsideTabs: List<Info>,
                val packagesByTab: Map<Int, List<Info>>,
            ) {
                data class Info(
                    val pkg: Package,
                    val isSelectedByDefault: Boolean,
                )

                /**
                 * Merges this [AvailablePackages] with another one. Note that this concatenates [packagesOutsideTabs],
                 * but replaces [packagesByTab] with the other one if this one is empty. This is because we expect
                 * only 1 tabs component in a single paywall.
                 */
                fun merge(with: AvailablePackages?): AvailablePackages =
                    AvailablePackages(
                        packagesOutsideTabs = packagesOutsideTabs + with?.packagesOutsideTabs.orEmpty(),
                        packagesByTab = packagesByTab.ifEmpty { with?.packagesByTab.orEmpty() },
                    )
            }

            data class SelectedPackageInfo(
                val rcPackage: Package,
                val currentlySubscribed: Boolean,
            )

            private val initialSelectedPackageOutsideTabs = packages.packagesOutsideTabs
                .firstOrNull { it.isSelectedByDefault }
                ?.pkg
            private val packagesOutsideTabs: Set<Package> = packages.packagesOutsideTabs
                .mapTo(mutableSetOf()) { it.pkg }
            private val tabsByPackage: Map<Package, Set<Int>> = mutableMapOf<Package, Set<Int>>().apply {
                packages.packagesByTab.forEach { (tabIndex, packages) ->
                    packages.forEach { packageInfo ->
                        val pkg = packageInfo.pkg
                        val tabIndices = getOrDefault(pkg, emptySet())
                        put(pkg, tabIndices + tabIndex)
                    }
                }
            }

            private var localeId by mutableStateOf(initialLocaleList.toLocaleId())

            val locale by derivedStateOf { localeId.toComposeLocale() }

            private val selectedPackageByTab = mutableStateMapOf<Int, Package?>().apply {
                putAll(
                    packages.packagesByTab.mapValues { (_, packages) ->
                        packages.firstOrNull { it.isSelectedByDefault }?.pkg
                    },
                )
            }

            var selectedTabIndex by mutableIntStateOf(initialSelectedTabIndex ?: 0)
                private set

            private val initialSelectedPackage = initialSelectedPackageOutsideTabs
                ?: initialSelectedTabIndex?.let { selectedPackageByTab[it] }

            private var selectedPackage by mutableStateOf(initialSelectedPackage)

            val selectedPackageInfo by derivedStateOf {
                selectedPackage?.let { rcPackage ->
                    SelectedPackageInfo(
                        rcPackage = rcPackage,
                        currentlySubscribed = rcPackage.currentlySubscribed(
                            activelySubscribedProductIdentifiers = activelySubscribedProductIds,
                            nonSubscriptionProductIdentifiers = purchasedNonSubscriptionProductIds,
                        ),
                    )
                }
            }

            val mostExpensivePricePerMonthMicros by derivedStateOf {
                (packages.packagesOutsideTabs + packages.packagesByTab[selectedTabIndex].orEmpty())
                    .mostExpensivePricePerMonthMicros()
            }

            val currentDate: Date
                get() = dateProvider()

            var actionInProgress by mutableStateOf(false)
                private set

            fun update(
                localeList: FrameworkLocaleList? = null,
                selectedTabIndex: Int? = null,
                actionInProgress: Boolean? = null,
            ) {
                if (localeList != null) localeId = LocaleList(localeList.toLanguageTags()).toLocaleId()

                if (selectedTabIndex != null) {
                    this.selectedTabIndex = selectedTabIndex
                    // If our currently selected package exists outside of tabs, we don't have to change the selected
                    // package when the tab changes.
                    if (packagesOutsideTabs.contains(selectedPackage)) return

                    selectedPackage = selectedPackageByTab[selectedTabIndex] ?: initialSelectedPackageOutsideTabs
                }

                if (actionInProgress != null) this.actionInProgress = actionInProgress
            }

            fun update(selectedPackage: Package) {
                this.selectedPackage = selectedPackage

                // Check if the package (also) exists on the currently selected tab. We need to remember this so we can
                // reselect this package when the user navigates away and back to the current tab.
                val currentTabIndex = selectedTabIndex
                val tabsWithThisPackage = tabsByPackage[selectedPackage]
                val currentTabContainsThisPackage = tabsWithThisPackage?.contains(currentTabIndex) == true
                if (currentTabContainsThisPackage) selectedPackageByTab[currentTabIndex] = selectedPackage
            }

            private fun LocaleList.toLocaleId(): LocaleId =
                // Configured locales take precedence over the default one.
                map { it.toLocaleId() }.plus(locales.head)
                    // Find the first locale we have a LocalizationDictionary for.
                    .firstNotNullOf { locale -> locales.getBestMatch(locale) }

            private fun List<AvailablePackages.Info>.mostExpensivePricePerMonthMicros(): Long? =
                asSequence()
                    .map { info -> info.pkg.product }
                    .mapNotNull { product -> product.pricePerMonth() }
                    .maxByOrNull { price -> price.amountMicros }
                    ?.amountMicros
        }
    }
}

internal fun PaywallState.loadedLegacy(): PaywallState.Loaded.Legacy? {
    return when (val state = this) {
        is PaywallState.Error -> null
        is PaywallState.Loaded -> when (state) {
            is PaywallState.Loaded.Legacy -> state
            is PaywallState.Loaded.Components -> null
        }

        is PaywallState.Loading -> null
    }
}

internal val PaywallState.Loaded.Legacy.selectedLocalization: ProcessedLocalizedConfiguration
    get() = selectedPackage.value.localization

internal val PaywallState.Loaded.Legacy.currentColors: TemplateConfiguration.Colors
    @Composable @ReadOnlyComposable
    get() = templateConfiguration.getCurrentColors()

internal val PaywallState.Loaded.Legacy.isInFullScreenMode: Boolean
    get() = templateConfiguration.mode.isFullScreen
