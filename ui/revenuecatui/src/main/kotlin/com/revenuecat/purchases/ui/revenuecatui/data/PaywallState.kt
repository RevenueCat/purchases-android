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
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabSelection
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsScope
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
import java.util.IllformedLocaleException
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
                val tabsScopes: List<TabsScope> = emptyList(),
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
                    val tab: TabSelection? = null,
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

                fun merge(with: AvailablePackages?): AvailablePackages = copy(
                    packagesOutsideTabs = packagesOutsideTabs + with?.packagesOutsideTabs.orEmpty(),
                    packagesByTab = (packagesByTab.keys + with?.packagesByTab.orEmpty().keys).associateWith {
                        packagesByTab[it].orEmpty() + with?.packagesByTab?.get(it).orEmpty()
                    },
                    tabsScopes = tabsScopes + with?.tabsScopes.orEmpty(),
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

            var selectedTabIndex by mutableIntStateOf(initialSelectedTabIndex ?: 0)
                private set

            private val mainTabsScope = packages.tabsScopes.firstOrNull { it.sheetPresentationKey == null }

            private val selectedTabs = mutableStateMapOf<TabsScope, Int>()

            fun selectedTabIndex(scope: TabsScope?): Int = when (scope) {
                null, mainTabsScope -> selectedTabIndex
                else -> selectedTabs[scope] ?: scope.defaultTabIndex
            }

            private fun tabIsActive(tab: TabSelection, override: TabSelection? = null): Boolean {
                val index = if (override != null && tab.scope === override.scope) {
                    override.index
                } else {
                    selectedTabIndex(tab.scope)
                }
                return tab.index == index && (tab.scope?.parent?.let { tabIsActive(it, override) } ?: true)
            }

            private fun activeTabSelections(): List<TabSelection> =
                if (packages.tabsScopes.isEmpty()) {
                    listOf(TabSelection(null, selectedTabIndex))
                } else {
                    packages.tabsScopes.map { TabSelection(it, selectedTabIndex(it)) }.filter { tabIsActive(it) }
                }

            private fun tabPackages(tab: TabSelection? = null): List<AvailablePackages.Info> =
                packages.packagesByTab.flatMap { (index, infos) ->
                    infos.filter { info ->
                        val candidate = info.tab ?: TabSelection(null, index)
                        tabIsActive(candidate, tab) && (tab == null || belongsToTab(candidate, tab))
                    }
                }

            private fun belongsToTab(candidate: TabSelection, tab: TabSelection): Boolean =
                candidate == tab || candidate.scope?.parent?.let { belongsToTab(it, tab) } == true

            private val currentTab: TabSelection
                get() = TabSelection(mainTabsScope, selectedTabIndex)

            private val initialVisibilityContext = ComponentVisibilityContext(
                screenCondition = ScreenCondition.COMPACT,
                windowDpSize = null,
                selectedPackage = { null },
                selectedTabIndex = { selectedTabIndex },
                scopedTabIndex = { selectedTabIndex(it) },
                customVariables = mergedCustomVariables,
                selectedOfferEligibility = { OfferEligibility.Ineligible },
                stateReader = stateStore::currentValueOrDefault,
                currentDate = dateProvider(),
            )

            private val initialSelectedPackageUniqueId = PackageSelectionResolver(
                packages = packages,
                selectedTabIndex = initialSelectedTabIndex ?: 0,
                presentedSheet = initialSheetState.presentationKey.takeIf { initialSheetState.visible },
                context = initialVisibilityContext,
                activeTabPackages = tabPackages(),
            ).initialSelection()

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
                val chosen = if (storefrontCountryCode.isNullOrBlank()) {
                    locale.toJavaLocale()
                } else {
                    val deviceLanguageCode = locale.language.lowercase()

                    val fallbackLocale = Locale.Builder()
                        .setLocale(locale.toJavaLocale())
                        .setRegion(storefrontCountryCode.uppercase())
                        .build()

                    // Use a storefront locale with the same number format when possible, preserving its currency
                    // symbol (for example, ￥ in Japan). Otherwise, keep the paywall language's number format.
                    availableStorefrontCountryLocalesByLanguage[deviceLanguageCode]
                        ?: availableStorefrontCountryLocalesByLanguage.values.firstOrNull {
                            it.hasSameNumberFormatAs(fallbackLocale)
                        }
                        ?: fallbackLocale
                }
                chosen.withNumberingSystemOverride(purchases.preferredUILocaleOverride).toComposeLocale()
            }

            private val selectedPackageByTab = mutableStateMapOf<TabSelection, String?>().apply {
                if (!initialSheetState.visible) {
                    activeTabSelections().forEach { tab ->
                        if (tabPackages(tab).any {
                                it.sheetPresentationKey == null && it.uniqueId == initialSelectedPackageUniqueId
                            }
                        ) {
                            put(tab, initialSelectedPackageUniqueId)
                        }
                    }
                }
            }

            private var selectedPackageUniqueId by mutableStateOf(initialSelectedPackageUniqueId)

            private data class PackageSelectionBeforeSheet(val uniqueId: String?)

            private var packageSelectionBeforeSheet: PackageSelectionBeforeSheet? = null

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
                val presentation = sheet.presentationKey.takeIf { sheet.visible }
                val outside = if (mainTabsScope == null) {
                    packages.packagesOutsideTabs
                } else {
                    packages.packagesOutsideTabs.inPresentation(presentation)
                }
                (outside + tabPackages().inPresentation(presentation)).mostExpensivePricePerMonthMicros()
            }

            data class TextPackageContext(
                val rcPackage: Package?,
                val subscriptionOption: SubscriptionOption?,
                val mostExpensivePricePerMonthMicros: Long?,
            )

            fun textPackageContext(
                tabIndex: Int?,
                discountTabIndex: Int?,
                fallbackPackage: Package?,
                fallbackSubscriptionOption: SubscriptionOption?,
                tabsScope: TabsScope? = null,
            ): TextPackageContext {
                if (discountTabIndex != null) {
                    return TextPackageContext(
                        fallbackPackage,
                        fallbackSubscriptionOption,
                        tabPackages(TabSelection(tabsScope ?: mainTabsScope, discountTabIndex))
                            .filter { it.sheetPresentationKey == null }
                            .mostExpensivePricePerMonthMicros(),
                    )
                }
                val tab = tabIndex?.let { TabSelection(tabsScope ?: mainTabsScope, it) }
                val tabPackages = tab?.let { tabPackages(it) }.orEmpty()
                val packageInfo = tabPackages.find { it.uniqueId == selectedPackageByTab[tab] }
                    ?: tabPackages.defaultSelection(visibilityContext(windowDpSize = null, tab = tab))
                return if (packageInfo != null) {
                    TextPackageContext(
                        packageInfo.pkg,
                        packageInfo.resolvedOffer?.subscriptionOption,
                        tabPackages.filter { it.sheetPresentationKey == null }
                            .mostExpensivePricePerMonthMicros(),
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

            fun selectTab(
                index: Int,
                tabId: String?,
                stateUpdates: List<StateUpdate>,
                tabsScope: TabsScope? = null,
            ) {
                if (index == selectedTabIndex(tabsScope)) return
                if (tabId != null) stateStore.applyUpdates(stateUpdates, payload = JsonPrimitive(tabId))
                if (tabsScope == null || tabsScope === mainTabsScope) {
                    selectedTabIndex = index
                } else {
                    selectedTabs[tabsScope] = index
                }
                val tab = TabSelection(tabsScope ?: mainTabsScope, index)
                val presentation = sheet.presentationKey.takeIf { sheet.visible }
                val candidates = tabPackages().inPresentation(presentation)
                val resolved = selectionResolver(tab = tab).switchTab(
                    current = selectedPackageUniqueId,
                    remembered = selectedPackageByTab[tab],
                ) ?: candidates.defaultSelection(
                    visibilityContext(windowDpSize = null),
                    packages.packagesOutsideTabs.inPresentation(presentation) + candidates,
                )?.uniqueId
                selectedPackageUniqueId = resolved
                if (resolved != null) update(selectedPackageUniqueId = resolved)
            }

            fun update(
                localeList: FrameworkLocaleList? = null,
                selectedTabIndex: Int? = null,
                clickScopedActionInProgress: Boolean? = null,
            ) {
                if (localeList != null) localeId = LocaleList(localeList.toLanguageTags()).toLocaleId()

                if (selectedTabIndex != null) selectTab(selectedTabIndex, null, emptyList())

                if (clickScopedActionInProgress != null) {
                    this.clickScopedActionInProgress = clickScopedActionInProgress
                }
            }

            fun update(selectedPackageUniqueId: String) {
                this.selectedPackageUniqueId = selectedPackageUniqueId

                val presentation = sheet.presentationKey.takeIf { sheet.visible }
                activeTabSelections().filter { it.scope?.sheetPresentationKey === presentation }.forEach { tab ->
                    if (tabPackages(tab).any {
                            it.sheetPresentationKey === presentation && it.uniqueId == selectedPackageUniqueId
                        }
                    ) {
                        selectedPackageByTab[tab] = selectedPackageUniqueId
                    }
                }
            }

            private fun selectionResolver(
                windowDpSize: DpSize? = paywallBoundsDp,
                presentedSheet: SheetPresentationKey? = sheet.presentationKey.takeIf { sheet.visible },
                tab: TabSelection? = null,
            ): PackageSelectionResolver {
                val candidates = tabPackages(tab)
                return PackageSelectionResolver(
                    packages = packages,
                    selectedTabIndex = selectedTabIndex,
                    presentedSheet = presentedSheet,
                    context = visibilityContext(windowDpSize),
                    activeTabPackages = candidates,
                    otherTabPackages = if (tab == null) emptyList() else tabPackages().filterNot { it in candidates },
                )
            }

            private fun visibilityContext(
                windowDpSize: DpSize?,
                tab: TabSelection? = null,
            ) = ComponentVisibilityContext(
                screenCondition = windowScreenCondition,
                windowDpSize = windowDpSize,
                selectedPackage = { selectedPackageInfo },
                selectedTabIndex = { tab?.index ?: selectedTabIndex },
                scopedTabIndex = { if (it === tab?.scope) tab.index else selectedTabIndex(it) },
                customVariables = mergedCustomVariables,
                selectedOfferEligibility = { selectedOfferEligibility },
                stateReader = stateStore::currentValueOrDefault,
                currentDate = dateProvider(),
            )

            fun selectPackageForSheetEntry() {
                // Entry arbitration is independent of measurement and must not update the underlying tab memory.
                if (packageSelectionBeforeSheet == null) {
                    packageSelectionBeforeSheet = PackageSelectionBeforeSheet(selectedPackageUniqueId)
                }
                selectedPackageUniqueId = selectionResolver().enterSheet(selectionBeforeSheetAtCurrentBounds())
            }

            fun restorePackageAfterSheetDismiss() {
                if (packageSelectionBeforeSheet == null) return
                selectedPackageUniqueId = selectionBeforeSheetAtCurrentBounds()
                packageSelectionBeforeSheet = null
            }

            private fun selectionBeforeSheetAtCurrentBounds(): String? {
                val selection = packageSelectionBeforeSheet ?: return null
                return if (paywallBoundsDp == null) {
                    selection.uniqueId
                } else {
                    selectionResolver(presentedSheet = null).reconcile(
                        current = selection.uniqueId,
                        remembered = selectedPackageByTab[currentTab],
                    )
                }
            }

            fun reconcileSelectionForWindowSize(windowDpSize: DpSize?) {
                if (windowDpSize == null) return
                val resolved = selectionResolver(windowDpSize).reconcile(
                    current = selectedPackageUniqueId,
                    remembered = selectedPackageByTab[currentTab],
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
                    remembered = selectedPackageByTab[currentTab],
                )

            fun peekSelectedPackageInfoAfterSheetDismiss(): SelectedPackageInfo? {
                packageSelectionBeforeSheet?.let {
                    return selectionBeforeSheetAtCurrentBounds()?.let { findPackageInfoByUniqueId(it)?.selectionInfo() }
                        ?: defaultPackageInfo
                }
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

/**
 * Copies an explicit Unicode numbering system (`nu`) from [preferredUILocaleOverride] onto this locale.
 * Overrides without that keyword, including ordinary language tags, are left unchanged.
 */
internal fun Locale.withNumberingSystemOverride(preferredUILocaleOverride: String?): Locale {
    val numberingSystem = preferredUILocaleOverride
        ?.let { Locale.forLanguageTag(it.replace('_', '-')).getUnicodeLocaleType("nu") }
        ?: return this
    return try {
        Locale.Builder()
            .setLocale(this)
            .setUnicodeLocaleKeyword("nu", numberingSystem)
            .build()
    } catch (@Suppress("SwallowedException") e: IllformedLocaleException) {
        this
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
