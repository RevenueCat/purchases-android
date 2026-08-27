package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.LocaleList
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ConditionContext
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPackagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.buildPresentedPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.composables.SimpleSheetState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.ProcessedLocalizedConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptySet
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallWarning
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
            val validationWarning: PaywallWarning? = null,
        ) : Loaded {

            constructor(
                offering: Offering,
                templateConfiguration: TemplateConfiguration,
                selectedPackage: TemplateConfiguration.PackageInfo,
                shouldDisplayDismissButton: Boolean,
                validationWarning: PaywallWarning? = null,
            ) :
                this(
                    offering,
                    templateConfiguration,
                    mutableStateOf(selectedPackage),
                    shouldDisplayDismissButton,
                    validationWarning,
                )

            fun selectPackage(packageInfo: TemplateConfiguration.PackageInfo) {
                selectedPackage.value = packageInfo
            }
        }

        @Suppress("LongParameterList")
        @Stable
        class Components(
            val stack: ComponentStyle,
            val header: ComponentStyle?,
            val stickyFooter: ComponentStyle?,
            val background: BackgroundStyles,
            val mainStackHasHeroImage: Boolean = false,
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
            val storefrontCountryCode: String?,
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
            /**
             * Presentation-session store for state-driven paywalls, seeded from the paywall's declared state defaults.
             */
            val stateStore: PaywallStateStore = PaywallStateStore(emptyMap()),
            /** The view model's gate, so every step of a workflow reads the one flag. */
            private val viewModelActionInProgress: State<Boolean> = mutableStateOf(false),
        ) : Loaded {

            /**
             * Custom variables merged from dashboard defaults and developer-provided overrides.
             * Developer-provided values take precedence.
             */
            val mergedCustomVariables: Map<String, CustomVariableValue> =
                defaultCustomVariables + customVariables

            val store: Store get() = purchases.store

            data class AvailablePackages(
                val packagesOutsideTabs: List<Info>,
                val packagesByTab: Map<Int, List<Info>>,
            ) {
                data class Info(
                    val pkg: Package,
                    val isSelectedByDefault: Boolean,
                    val resolvedOffer: ResolvedOffer? = null,
                    /**
                     * The package component's static visibility and the overrides that can change it.
                     * Selection has to evaluate these: a package hidden by a rule must not be chosen as
                     * the default, or nothing ends up selected on screen.
                     */
                    val visible: Boolean = true,
                    val visibilityOverrides: List<PresentedOverride<PresentedPackagePartial>> = emptyList(),
                    val offerEligibility: OfferEligibility? = null,
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
                val offerEligibility: OfferEligibility,
            )

            private val initialSelectedPackageOutsideTabs = packages.packagesOutsideTabs
                .firstOrNull { it.isSelectedByDefault && it.resolvesVisible(mergedCustomVariables) }
                ?.uniqueId

            /**
             * Only used when a default *is* declared outside the tabs but a rule hid it, which would
             * otherwise leave nothing selected. A paywall that declares no default still starts unselected.
             */
            private val visibleFallbackForHiddenDefaultOutsideTabs = packages.packagesOutsideTabs
                .takeIf { infos -> infos.any { it.isSelectedByDefault } }
                ?.firstOrNull { it.resolvesVisible(mergedCustomVariables) }
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
                getAvailableStorefrontCountryLocalesByLanguage(storefrontCountryCode)
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
                        packagesList.defaultSelection(mergedCustomVariables)?.uniqueId
                    },
                )
            }

            var selectedTabIndex by mutableIntStateOf(initialSelectedTabIndex ?: 0)
                private set

            private val initialSelectedPackageUniqueId: String? = initialSelectedPackageOutsideTabs
                ?: selectedPackageByTab[selectedTabIndex]
                ?: packages.packagesByTab[selectedTabIndex]?.defaultSelection(mergedCustomVariables)?.uniqueId
                // Last, so a default declared inside a tab still wins.
                ?: visibleFallbackForHiddenDefaultOutsideTabs

            private var selectedPackageUniqueId by mutableStateOf(initialSelectedPackageUniqueId)

            private var defaultPackageInfo: SelectedPackageInfo? by mutableStateOf(null)

            internal fun setDefaultPackage(info: SelectedPackageInfo) {
                // Idempotency lock: default is set once and never overwritten, so back
                // navigation always shows the same content as the initial render.
                if (defaultPackageInfo == null) defaultPackageInfo = info
            }

            val selectedPackageInfo by derivedStateOf {
                val ownSelection = selectedPackageUniqueId?.let { uniqueId ->
                    findPackageInfoByUniqueId(uniqueId)?.let { info ->
                        SelectedPackageInfo(
                            rcPackage = info.pkg,
                            resolvedOffer = info.resolvedOffer,
                            uniqueId = uniqueId,
                            offerEligibility = calculateOfferEligibility(info.resolvedOffer, info.pkg),
                        )
                    }
                }
                ownSelection ?: defaultPackageInfo
            }

            private fun findPackageInfoByUniqueId(uniqueId: String): AvailablePackages.Info? {
                return packages.packagesOutsideTabs.find { it.uniqueId == uniqueId }
                    ?: packages.packagesByTab.values.flatten().find { it.uniqueId == uniqueId }
            }

            val selectedOfferEligibility by derivedStateOf {
                selectedPackageInfo?.offerEligibility ?: OfferEligibility.Ineligible
            }

            val mostExpensivePricePerMonthMicros by derivedStateOf {
                (packages.packagesOutsideTabs + packages.packagesByTab[selectedTabIndex].orEmpty())
                    .mostExpensivePricePerMonthMicros()
            }

            val currentDate: Date
                get() = dateProvider()

            val appUserID: String
                get() = purchases.appUserID

            /**
             * The measured height of the header overlay in pixels. Set during the layout phase by
             * the custom Layout in [LoadedPaywallComponents] so that ZLayer stacks can read it
             * during their own layout phase (via [Modifier.layout]) to offset non-hero children
             * below the header — all in a single pass, without recomposition.
             */
            @get:JvmSynthetic
            var headerHeightPx: Int = 0
                @JvmSynthetic internal set

            /**
             * The measured height of the sticky-footer overlay in pixels. Set during the layout phase by
             * the custom Layout in [LoadedPaywallComponents], so main content can reserve bottom clearance
             * (via [Modifier.footerBottomPadding]) in the same pass, without recomposition.
             */
            @get:JvmSynthetic
            var footerHeightPx: Int = 0
                @JvmSynthetic internal set

            /** Raised and cleared by the button, for the actions that begin and end with the click. */
            var clickScopedActionInProgress by mutableStateOf(false)
                private set

            /**
             * Read live rather than mirrored, so an action that outlives the button that started it keeps
             * the paywall disabled after [clickScopedActionInProgress] is cleared by the click's cancellation.
             */
            val actionInProgress: Boolean
                get() = viewModelActionInProgress.value || clickScopedActionInProgress

            val sheet = initialSheetState

            fun update(
                localeList: FrameworkLocaleList? = null,
                selectedTabIndex: Int? = null,
                clickScopedActionInProgress: Boolean? = null,
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
                        ?: packages.packagesByTab[selectedTabIndex]
                            ?.defaultSelection(mergedCustomVariables)
                            ?.also { selection ->
                                if (!selection.isSelectedByDefault) {
                                    Logger.w(
                                        "Could not find a visible default package for tab $selectedTabIndex. " +
                                            "Using the first visible package instead.",
                                    )
                                }
                            }
                            ?.uniqueId
                        // Nothing in the tab renders, so fall back outside it rather than clearing.
                        ?: visibleFallbackForHiddenDefaultOutsideTabs
                }

                if (clickScopedActionInProgress != null) {
                    this.clickScopedActionInProgress = clickScopedActionInProgress
                }
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
                selectedPackageUniqueId = peekDefaultPackageUniqueIdAfterSheetDismiss()
            }

            /** The package the current tab should fall back to, which is also what a reset restores. */
            fun peekDefaultPackageUniqueIdAfterSheetDismiss(): String? {
                val tabPackages = packages.packagesByTab[selectedTabIndex]
                // A default authored outside the tabs outranks a tab package that was never authored as
                // one, so the tab's own default is consulted first and its first visible package last.
                return tabPackages?.authoredDefaultIfVisible(mergedCustomVariables)?.uniqueId
                    ?: initialSelectedPackageOutsideTabs
                    ?: selectedPackageByTab[selectedTabIndex]
                    ?: tabPackages?.firstVisible(mergedCustomVariables)?.uniqueId
                    ?: visibleFallbackForHiddenDefaultOutsideTabs
            }

            fun peekSelectedPackageInfoAfterSheetDismiss(): SelectedPackageInfo? {
                val uid = peekDefaultPackageUniqueIdAfterSheetDismiss()
                val info = uid?.let { findPackageInfoByUniqueId(it) }
                return if (uid != null && info != null) {
                    SelectedPackageInfo(
                        rcPackage = info.pkg,
                        resolvedOffer = info.resolvedOffer,
                        uniqueId = uid,
                        offerEligibility = calculateOfferEligibility(info.resolvedOffer, info.pkg),
                    )
                } else {
                    null
                }
            }

            /**
             * Default package for the current tab / root context (aligned with [resetToDefaultPackage]).
             */
            fun defaultPackageForPackageRowAnalytics(): Package? {
                val uid = peekDefaultPackageUniqueIdAfterSheetDismiss() ?: return null
                return findPackageInfoByUniqueId(uid)?.pkg
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

/**
 * Returns the available locales for the storefront country keyed by language.
 * POSIX locales are excluded because their currency format can contain spacing that differs from the store format.
 */
internal fun getAvailableStorefrontCountryLocalesByLanguage(
    storefrontCountryCode: String?,
    availableLocales: Array<Locale> = Locale.getAvailableLocales(),
): Map<String, Locale> =
    if (storefrontCountryCode.isNullOrBlank()) {
        emptyMap()
    } else {
        availableLocales
            .filter { locale ->
                locale.country.equals(storefrontCountryCode, ignoreCase = true) &&
                    !locale.variant.equals("POSIX", ignoreCase = true)
            }
            .associateBy { it.language.lowercase() }
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

/**
 * Whether this package renders, evaluating the same overrides the renderer does.
 *
 * Resolved as unselected with no selected package, because selection is what's being decided: pinning
 * those keeps resolution independent of its own result, so a paywall with `selected` or
 * `selected_package` visibility rules can't oscillate.
 *
 * Note this only covers the package component's own rules. A package hidden solely by an enclosing
 * stack's rule still resolves visible here.
 */
private fun PaywallState.Loaded.Components.AvailablePackages.Info.resolvesVisible(
    customVariables: Map<String, CustomVariableValue>,
): Boolean =
    visibilityOverrides.buildPresentedPartial(
        windowSize = ScreenCondition.COMPACT,
        offerEligibility = offerEligibility ?: OfferEligibility.Ineligible,
        state = ComponentViewState.DEFAULT,
        conditionContext = ConditionContext(selectedPackageId = null, customVariables = customVariables),
    )?.partial?.visible ?: visible

/**
 * The package that should start selected, in document order: the authored default if it renders,
 * otherwise the first package that does.
 */
private fun List<PaywallState.Loaded.Components.AvailablePackages.Info>.defaultSelection(
    customVariables: Map<String, CustomVariableValue>,
): PaywallState.Loaded.Components.AvailablePackages.Info? =
    authoredDefaultIfVisible(customVariables) ?: firstVisible(customVariables)

/** The package authored as the default, only when it renders. */
private fun List<PaywallState.Loaded.Components.AvailablePackages.Info>.authoredDefaultIfVisible(
    customVariables: Map<String, CustomVariableValue>,
): PaywallState.Loaded.Components.AvailablePackages.Info? =
    firstOrNull { it.isSelectedByDefault && it.resolvesVisible(customVariables) }

private fun List<PaywallState.Loaded.Components.AvailablePackages.Info>.firstVisible(
    customVariables: Map<String, CustomVariableValue>,
): PaywallState.Loaded.Components.AvailablePackages.Info? =
    firstOrNull { it.resolvesVisible(customVariables) }
