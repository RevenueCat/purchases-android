package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.SimpleSheetState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptySet
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.ui.revenuecatui.helpers.createLocaleFromString
import com.revenuecat.purchases.ui.revenuecatui.isFullScreen
import java.util.Date
import java.util.Locale
import android.os.LocaleList as FrameworkLocaleList

@Stable
internal sealed interface PaywallState {

    @Immutable
    object Loading : PaywallState

    @Immutable
    data class Error(val errorMessage: String) : PaywallState {
        init {
            Logger.e("Paywall transitioned to error state: $errorMessage")
        }
    }

    @Stable
    sealed interface Loaded : PaywallState {
        val offering: Offering

        @Stable
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
            val variableDataProvider: VariableDataProvider,
            override val offering: Offering,
            /**
             * All locales that this paywall supports, with `locales.head` being the default one.
             */
            private val locales: NonEmptySet<LocaleId>,
            private val storefrontCountryCode: String?,
            private val dateProvider: () -> Date,
            private val packages: AvailablePackages,
            /**
             * Custom variables provided by the SDK user at runtime.
             */
            val customVariables: Map<String, CustomVariableValue> = emptyMap(),
            /**
             * Default custom variables from the dashboard configuration.
             */
            val defaultCustomVariables: Map<String, CustomVariableValue> = emptyMap(),
            initialLocaleList: LocaleList = LocaleList.current,
            initialSelectedTabIndex: Int? = null,
            initialSheetState: SimpleSheetState = SimpleSheetState(),
            private val purchases: PurchasesType,
        ) : Loaded {

            data class AvailablePackages(
                val packagesOutsideTabs: List<Info>,
                val packagesByTab: Map<Int, List<Info>>,
            ) {
                data class Info(
                    val pkg: Package,
                    val isSelectedByDefault: Boolean,
                    val resolvedOffer: ResolvedOffer? = null,
                ) {
                    /**
                     * Unique identifier combining package ID and offer ID.
                     * This distinguishes multiple components referencing the same package
                     * but with different offer configurations.
                     */
                    val uniqueId: String = run {
                        val offerId = (resolvedOffer as? ResolvedOffer.ConfiguredOffer)?.option?.id
                        if (offerId != null) "${pkg.identifier}:$offerId" else pkg.identifier
                    }
                }

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

                val hasAnyPackages: Boolean
                    get() = packagesOutsideTabs.isNotEmpty() || packagesByTab.isNotEmpty()
            }

            data class SelectedPackageInfo(
                val rcPackage: Package,
                val resolvedOffer: ResolvedOffer? = null,
                val uniqueId: String,
            )

            private val initialSelectedPackageOutsideTabs = packages.packagesOutsideTabs
                .firstOrNull { it.isSelectedByDefault }
                ?.uniqueId
            private val packagesOutsideTabsUniqueIds: Set<String> = packages.packagesOutsideTabs
                .mapTo(mutableSetOf()) { it.uniqueId }
            private val tabsByUniqueId: Map<String, Set<Int>> = mutableMapOf<String, Set<Int>>().apply {
                packages.packagesByTab.forEach { (tabIndex, packagesList) ->
                    packagesList.forEach { packageInfo ->
                        val uniqueId = packageInfo.uniqueId
                        val tabIndices = getOrDefault(uniqueId, emptySet())
                        put(uniqueId, tabIndices + tabIndex)
                    }
                }
            }

            private var localeId by mutableStateOf(initialLocaleList.toLocaleId())

            // We find all available device locales with the same country as the storefront country.
            private val availableStorefrontCountryLocalesByLanguage: Map<String, Locale> by lazy {
                if (storefrontCountryCode.isNullOrBlank()) {
                    emptyMap()
                } else {
                    buildMap {
                        Locale.getAvailableLocales().forEach { availableLocale ->
                            if (availableLocale.country.equals(storefrontCountryCode, ignoreCase = true)) {
                                put(availableLocale.language.lowercase(), availableLocale)
                            }
                        }
                    }
                }
            }

            /**
             * The locale to use for the paywall's localized content, such as text.
             */
            val locale by derivedStateOf { localeId.toComposeLocale() }

            /**
             * The locale to use when formatting currencies. This corresponds to the user's storefront country, to
             * avoid discrepancies between calculated prices (per period) and the price coming directly from the store.
             */
            val currencyLocale by derivedStateOf {
                if (storefrontCountryCode.isNullOrBlank()) {
                    locale
                } else {
                    val deviceLanguageCode = locale.language.lowercase()

                    // We pick the one with the same language as the device if available. If not, we just pick the
                    // first. If the list is empty, we use the device locale with the storefront country.
                    val javaLocale = availableStorefrontCountryLocalesByLanguage[deviceLanguageCode]
                        ?: availableStorefrontCountryLocalesByLanguage.values.firstOrNull()
                        ?: Locale.Builder()
                            .setLocale(locale.toJavaLocale())
                            .setRegion(storefrontCountryCode.uppercase())
                            .build()

                    javaLocale.toComposeLocale()
                }
            }

            private val selectedPackageByTab = mutableStateMapOf<Int, String?>().apply {
                putAll(
                    packages.packagesByTab.mapValues { (_, packagesList) ->
                        packagesList.firstOrNull { it.isSelectedByDefault }?.uniqueId
                    },
                )
            }

            var selectedTabIndex by mutableIntStateOf(initialSelectedTabIndex ?: 0)
                private set

            private val initialSelectedPackageUniqueId: String? = initialSelectedPackageOutsideTabs
                ?: selectedPackageByTab[selectedTabIndex]
                ?: packages.packagesByTab[selectedTabIndex]?.firstOrNull()?.uniqueId

            private var selectedPackageUniqueId by mutableStateOf(initialSelectedPackageUniqueId)

            val selectedPackageInfo by derivedStateOf {
                selectedPackageUniqueId?.let { uniqueId ->
                    findPackageInfoByUniqueId(uniqueId)?.let { info ->
                        SelectedPackageInfo(
                            rcPackage = info.pkg,
                            resolvedOffer = info.resolvedOffer,
                            uniqueId = uniqueId,
                        )
                    }
                }
            }

            private fun findPackageInfoByUniqueId(uniqueId: String): AvailablePackages.Info? {
                return packages.packagesOutsideTabs.find { it.uniqueId == uniqueId }
                    ?: packages.packagesByTab.values.flatten().find { it.uniqueId == uniqueId }
            }

            val mostExpensivePricePerMonthMicros by derivedStateOf {
                (packages.packagesOutsideTabs + packages.packagesByTab[selectedTabIndex].orEmpty())
                    .mostExpensivePricePerMonthMicros()
            }

            val currentDate: Date
                get() = dateProvider()

            var actionInProgress by mutableStateOf(false)
                private set

            val sheet = initialSheetState

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
                    if (selectedPackageUniqueId != null &&
                        packagesOutsideTabsUniqueIds.contains(selectedPackageUniqueId)
                    ) {
                        return
                    }

                    selectedPackageUniqueId = selectedPackageByTab[selectedTabIndex]
                        ?: initialSelectedPackageOutsideTabs
                        ?: packages.packagesByTab[selectedTabIndex]?.firstOrNull()?.uniqueId?.also {
                            Logger.w(
                                "Could not find default package for tab $selectedTabIndex. " +
                                    "Using first package instead. " +
                                    "This could be caused by not having any package marked as selected by default.",
                            )
                        }
                }

                if (actionInProgress != null) this.actionInProgress = actionInProgress
            }

            fun update(selectedPackageUniqueId: String) {
                this.selectedPackageUniqueId = selectedPackageUniqueId

                // Check if the package (also) exists on the currently selected tab. We need to remember this so we can
                // reselect this package when the user navigates away and back to the current tab.
                val currentTabIndex = selectedTabIndex
                val tabsWithThisPackage = tabsByUniqueId[selectedPackageUniqueId]
                val currentTabContainsThisPackage = tabsWithThisPackage?.contains(currentTabIndex) == true
                if (currentTabContainsThisPackage) selectedPackageByTab[currentTabIndex] = selectedPackageUniqueId
            }

            fun resetToDefaultPackage() {
                selectedPackageUniqueId =
                    packages.packagesByTab[selectedTabIndex]?.firstOrNull { it.isSelectedByDefault }?.uniqueId
                        ?: initialSelectedPackageOutsideTabs
                        ?: selectedPackageByTab[selectedTabIndex]
            }

            private fun LocaleList.toLocaleId(): LocaleId {
                val preferredOverride = purchases.preferredUILocaleOverride
                val deviceLocales = map { it.toLocaleId() }.plus(locales.head)

                val allLocales = if (preferredOverride != null) {
                    // Parse preferred locale override and put it first in priority
                    val preferredLocaleId = try {
                        createLocaleFromString(preferredOverride).toComposeLocale().toLocaleId()
                    } catch (@Suppress("SwallowedException", "TooGenericExceptionCaught") e: Exception) {
                        // Fallback to null if preferred locale string is malformed
                        null
                    }
                    if (preferredLocaleId != null) {
                        listOf(preferredLocaleId) + deviceLocales
                    } else {
                        deviceLocales
                    }
                } else {
                    deviceLocales
                }

                // Find the first locale we have a LocalizationDictionary for.
                return allLocales.firstNotNullOf { locale -> locales.getBestMatch(locale) }
            }

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
