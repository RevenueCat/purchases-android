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
import androidx.compose.ui.unit.DpSize
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.storage.FileRepository
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentStyleIdentity
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentVisibilityContext
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPackagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.ResolvedHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.resolveLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toComposeLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toLocaleId
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageVisibilityPath
import com.revenuecat.purchases.ui.revenuecatui.components.style.SheetPresentationKey
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
import kotlinx.serialization.json.JsonPrimitive
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import android.os.LocaleList as FrameworkLocaleList

@Stable
internal sealed interface PaywallState {

    @Immutable
    object Loading : PaywallState

    @Immutable
    data class Error(val errorMessage: String, val error: PurchasesError? = null) : PaywallState {
        init {
            Logger.e("Paywall transitioned to error state: $errorMessage")
        }

        fun toPaywallResult(): PaywallResult.Error =
            PaywallResult.Error(error ?: PurchasesError(PurchasesErrorCode.UnknownError, errorMessage))
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

        @Suppress("LongParameterList", "TooManyFunctions")
        @Stable
        class Components(
            val stack: ComponentStyle,
            val header: ComponentStyle?,
            val stickyFooter: ComponentStyle?,
            val background: BackgroundStyles,
            mainStackHasHeroImage: Boolean = false,
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
            val workflowScreen: WorkflowScreenContext? = null,
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

            /**
             * The paywall's measured bounds in dp, which window size rules evaluate against —
             * a paywall in a sheet or pane sees its own size, not the app window's. Set by the
             * paywall root's measurement; null until first measure, matching iOS.
             */
            var paywallBoundsDp: DpSize? by mutableStateOf(null)
                @JvmSynthetic internal set

            var mainStackHasHeroImage by mutableStateOf(mainStackHasHeroImage)
                internal set

            internal var resolvedHeroInsets by mutableStateOf<Map<ComponentStyleIdentity, ResolvedHeroInsets>>(
                emptyMap(),
            )

            /**
             * The screen condition the renderer resolves size-class overrides with — derived from
             * the app window, not the paywall bounds, which can fall in different size classes in
             * a sheet or pane. Set alongside [paywallBoundsDp] by the paywall root's measurement.
             */
            var windowScreenCondition: ScreenCondition by mutableStateOf(ScreenCondition.COMPACT)
                @JvmSynthetic internal set

            val store: Store get() = purchases.store
            val fileRepositoryOverride: FileRepository? get() = purchases.fileRepositoryOverride

            /** A subset of the offering: packages the paywall never shows are not in it. */
            val paywallPackages: List<Package> by lazy {
                (packages.packagesOutsideTabs + packages.packagesByTab.toSortedMap().values.flatten())
                    .map { it.pkg }
                    .distinctBy { it.identifier }
            }

            data class AvailablePackages(
                val packagesOutsideTabs: List<Info>,
                val packagesByTab: Map<Int, List<Info>>,
            ) {
                data class Info(
                    val pkg: Package,
                    val isSelectedByDefault: Boolean,
                    override val resolvedOffer: ResolvedOffer? = null,
                    /**
                     * The package component's static visibility and the overrides that can change it.
                     * Selection has to evaluate these: a package hidden by a rule must not be chosen as
                     * the default, or nothing ends up selected on screen.
                     */
                    val visible: Boolean = true,
                    val visibilityOverrides: List<PresentedOverride<PresentedPackagePartial>> = emptyList(),
                    override val offerEligibility: OfferEligibility? = null,
                    /** Null for root content; otherwise identifies the sheet containing this component. */
                    val sheetPresentationKey: SheetPresentationKey? = null,
                    /** Null before styles are built; an empty list means there is no rendered path. */
                    val visibilityPaths: List<PackageVisibilityPath>? = null,
                ) : PackageContext {
                    override val rcPackage: Package get() = pkg
                    override val tabIndex: Int? get() = null

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

            private val initialVisibilityContext = ComponentVisibilityContext(
                screenCondition = ScreenCondition.COMPACT,
                windowDpSize = null,
                selectedPackage = { null },
                selectedTabIndex = { initialSelectedTabIndex ?: 0 },
                customVariables = mergedCustomVariables,
                selectedOfferEligibility = { OfferEligibility.Ineligible },
                stateReader = stateStore::currentValueOrDefault,
                currentDate = dateProvider(),
            )

            private val initialPackageCandidates = packages.packagesOutsideTabs +
                packages.packagesByTab[initialSelectedTabIndex ?: 0].orEmpty()

            private val initialSelectedPackageOutsideTabs = packages.packagesOutsideTabs
                .authoredDefaultSelection(initialVisibilityContext, initialPackageCandidates)
                ?.uniqueId

            /**
             * Only used when a default *is* declared outside the tabs but a rule hid it, which would
             * otherwise leave nothing selected. A paywall that declares no default still starts unselected.
             */
            private val visibleFallbackForHiddenDefaultOutsideTabs = packages.packagesOutsideTabs
                .takeIf { infos -> infos.any { it.isSelectedByDefault } }
                ?.firstOrNull { it.canSelect(initialVisibilityContext, initialPackageCandidates) }
                ?.uniqueId
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

                    val fallbackLocale = Locale.Builder()
                        .setLocale(locale.toJavaLocale())
                        .setRegion(storefrontCountryCode.uppercase())
                        .build()

                    // Use a storefront locale with the same number format when possible, preserving its currency
                    // symbol (for example, ￥ in Japan). Otherwise, keep the paywall language's number format.
                    val javaLocale = availableStorefrontCountryLocalesByLanguage[deviceLanguageCode]
                        ?: availableStorefrontCountryLocalesByLanguage.values.firstOrNull {
                            it.hasSameNumberFormatAs(fallbackLocale)
                        }
                        ?: fallbackLocale

                    javaLocale.toComposeLocale()
                }
            }

            private val selectedPackageByTab = mutableStateMapOf<Int, String?>().apply {
                // Other tabs must resolve their defaults after publishing their own state, on first visit.
                val index = initialSelectedTabIndex ?: 0
                packages.packagesByTab[index]?.let { packagesList ->
                    put(
                        index,
                        packagesList.defaultSelection(initialVisibilityContext, initialPackageCandidates)?.uniqueId,
                    )
                }
            }

            var selectedTabIndex by mutableIntStateOf(initialSelectedTabIndex ?: 0)
                private set

            private val initialSelectedPackageUniqueId: String? = initialSelectedPackageOutsideTabs
                ?: selectedPackageByTab[selectedTabIndex]
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

            data class TextPackageContext(
                val rcPackage: Package?,
                val subscriptionOption: SubscriptionOption?,
                val mostExpensivePricePerMonthMicros: Long?,
            )

            fun textPackageContext(
                tabIndex: Int?,
                fallbackPackage: Package?,
                fallbackSubscriptionOption: SubscriptionOption?,
            ): TextPackageContext {
                val tabPackages = packages.packagesByTab[tabIndex].orEmpty()
                val packageInfo = tabPackages.find { it.uniqueId == selectedPackageByTab[tabIndex] }
                    ?: tabPackages.defaultSelection(mergedCustomVariables)
                return if (packageInfo != null) {
                    TextPackageContext(
                        packageInfo.pkg,
                        packageInfo.resolvedOffer?.subscriptionOption,
                        tabPackages.mostExpensivePricePerMonthMicros(),
                    )
                } else {
                    TextPackageContext(fallbackPackage, fallbackSubscriptionOption, mostExpensivePricePerMonthMicros)
                }
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
             * (via [Modifier.footerBottomPadding]) in the same pass. This is observable so a later footer
             * resize invalidates the main content's otherwise unchanged measurement constraints.
             */
            @get:JvmSynthetic
            var footerHeightPx by mutableIntStateOf(0)
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

            fun selectTab(index: Int, tabId: String?, stateUpdates: List<StateUpdate>) {
                if (index != selectedTabIndex && tabId != null) {
                    // Visibility rules must see the destination tab's state before selection is resolved.
                    stateStore.applyUpdates(stateUpdates, payload = JsonPrimitive(tabId))
                }
                update(selectedTabIndex = index)
            }

            fun update(
                localeList: FrameworkLocaleList? = null,
                selectedTabIndex: Int? = null,
                clickScopedActionInProgress: Boolean? = null,
            ) {
                if (localeList != null) localeId = LocaleList(localeList.toLanguageTags()).toLocaleId()

                if (selectedTabIndex != null) {
                    this.selectedTabIndex = selectedTabIndex
                    val tabPackages = packages.packagesByTab[selectedTabIndex].orEmpty()
                    val resolved = selectionResolver().switchTab(
                        current = selectedPackageUniqueId,
                        remembered = selectedPackageByTab[selectedTabIndex],
                    ) ?: tabPackages.defaultSelection(
                        // Preserve the tab's initial choice when the current bounds hide every candidate.
                        visibilityContext(windowDpSize = null),
                        packages.packagesOutsideTabs + tabPackages,
                    )?.uniqueId
                    selectedPackageUniqueId = resolved
                    if (resolved != null) update(selectedPackageUniqueId = resolved)
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

            private fun selectionResolver(windowDpSize: DpSize? = paywallBoundsDp) = PackageSelectionResolver(
                packages = packages,
                selectedTabIndex = selectedTabIndex,
                presentedSheet = sheet.presentationKey.takeIf { sheet.visible },
                context = visibilityContext(windowDpSize),
            )

            private fun visibilityContext(windowDpSize: DpSize?) = ComponentVisibilityContext(
                screenCondition = windowScreenCondition,
                windowDpSize = windowDpSize,
                selectedPackage = { selectedPackageInfo },
                selectedTabIndex = { selectedTabIndex },
                customVariables = mergedCustomVariables,
                selectedOfferEligibility = { selectedOfferEligibility },
                stateReader = stateStore::currentValueOrDefault,
                currentDate = dateProvider(),
            )

            fun reconcileSelectionForWindowSize(windowDpSize: DpSize?) {
                if (windowDpSize == null) return
                val resolved = selectionResolver(windowDpSize).reconcile(
                    current = selectedPackageUniqueId,
                    remembered = selectedPackageByTab[selectedTabIndex],
                )
                if (resolved != null && resolved != selectedPackageUniqueId) {
                    update(selectedPackageUniqueId = resolved)
                }
            }

            fun resetToDefaultPackage() {
                selectedPackageUniqueId = peekDefaultPackageUniqueIdAfterSheetDismiss()
            }

            fun peekDefaultPackageUniqueIdAfterSheetDismiss(windowDpSize: DpSize? = paywallBoundsDp): String? =
                selectionResolver(windowDpSize).reset(
                    current = selectedPackageUniqueId,
                    remembered = selectedPackageByTab[selectedTabIndex],
                )

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

                return locales.resolveLocale(
                    preferredLocales = allLocales,
                    defaultLocale = locales.head,
                )
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

private const val NUMBER_FORMAT_SAMPLE = 1_234_567.89

private fun Locale.hasSameNumberFormatAs(other: Locale): Boolean =
    NumberFormat.getNumberInstance(this).format(NUMBER_FORMAT_SAMPLE) ==
        NumberFormat.getNumberInstance(other).format(NUMBER_FORMAT_SAMPLE)

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
