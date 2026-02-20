package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.TimelineComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCarouselPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTabsPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelineItemPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelinePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedVideoPartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.imageForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.stringForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.videoForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.getFontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.recoverFromFontAliasError
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState.Loaded.Components.AvailablePackages
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.extensions.toOrientation
import com.revenuecat.purchases.ui.revenuecatui.extensions.toPageControlStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.PromoOfferResolver
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorIfNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatten
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptyListOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import java.util.Date

@Suppress("TooManyFunctions", "LargeClass")
@Immutable
internal class StyleFactory(
    private val localizations: NonEmptyMap<LocaleId, LocalizationDictionary>,
    private val colorAliases: Map<ColorAlias, ColorScheme>,
    private val fontAliases: Map<FontAlias, FontSpec>,
    private val variableLocalizations: NonEmptyMap<LocaleId, NonEmptyMap<VariableLocalizationKey, String>>,
    private val offering: Offering,
) {

    internal companion object {
        private const val DEFAULT_SPACING = 0f
        private const val DEFAULT_VISIBILITY = true
        internal val DEFAULT_SHAPE = Shape.Rectangle()
    }

    private data class StyleFactoryScope(
        var packageInfo: AvailablePackages.Info? = null,
        var tabControl: TabControlStyle? = null,
        /**
         * If this is non-null, it means the branch currently being built is inside a tab control component. Every tab
         * in a tabs component will contain a tab control component. A tab control component is often implemented as a
         * segmented button, with each segment of the button switching to the tab indicated by that segment's tab
         * control index.
         */
        var tabControlIndex: Int? = null,
        /**
         * If this is non-null, it means the branch currently being built is inside a tab component.
         */
        var tabIndex: Int? = null,
        /**
         * If this is non-null, it means the branch currently being built is inside a countdown component.
         */
        var countdownDate: Date? = null,
        /**
         * Indicates how the countdown should count (from days, hours, or minutes).
         */
        var countFrom: CountdownComponent.CountFrom = CountdownComponent.CountFrom.DAYS,
        /**
         * Keeps the predicates we're actively using to count components.
         */
        private val countPredicates: MutableMap<Int, (PaywallComponent) -> Boolean> = mutableMapOf(),
        /**
         * The counts we're actively keeping track of.
         */
        private val countValues: MutableMap<Int, Int> = mutableMapOf(),
    ) {
        data class WithCount<T>(
            val value: T,
            val count: Int,
        )

        private class WindowInsetsState {
            /**
             * Whether the current component should apply the top window insets. This field is reset when it is read,
             * as it should only be set on a single component.
             */
            var applyTopWindowInsets = false
                get() {
                    val value = field
                    field = false
                    return value
                }

            /**
             * Whether the current component should ignore the top window insets. This field is reset when it is read,
             * as it should only be set on a single component.
             */
            var ignoreTopWindowInsets = false
                get() {
                    val value = field
                    field = false
                    return value
                }

            /**
             * Whether we have applied the top window insets to any component.
             */
            var topWindowInsetsApplied = false

            /**
             * We're only interested in the first non-container component. After that, we can stop looking.
             */
            private var stillLookingForHeaderMedia = true

            /**
             * This will be called for every component in the tree, and will determine whether we have a header image
             * or video that needs special top-window-insets treatment. A header image is found if the first
             * non-container component is an image component with a Fill width and a ZLayer parent stack.
             */
            fun handleHeaderMediaViewWindowInsets(component: PaywallComponent) {
                when (component) {
                    is StackComponent -> if (stillLookingForHeaderMedia) {
                        applyTopWindowInsets = when (component.dimension) {
                            is Dimension.ZLayer -> {
                                topWindowInsetsApplied = component.components.firstOrNull()?.isHeaderMedia == true
                                topWindowInsetsApplied
                            }

                            is Dimension.Horizontal,
                            is Dimension.Vertical,
                            -> false
                        }
                    }

                    is ImageComponent -> {
                        if (stillLookingForHeaderMedia) {
                            ignoreTopWindowInsets = component.isHeaderImage
                        }
                        stillLookingForHeaderMedia = false
                    }

                    is VideoComponent -> {
                        if (stillLookingForHeaderMedia) {
                            ignoreTopWindowInsets = component.isHeaderVideo
                        }
                        stillLookingForHeaderMedia = false
                    }

                    else -> stillLookingForHeaderMedia = false
                }
            }

            private val PaywallComponent.isHeaderMedia: Boolean
                get() = isHeaderImage || isHeaderVideo

            private val PaywallComponent.isHeaderImage: Boolean
                get() = this is ImageComponent &&
                    when (size.width) {
                        is SizeConstraint.Fill -> true
                        is SizeConstraint.Fit,
                        is SizeConstraint.Fixed,
                        -> false
                    }

            private val PaywallComponent.isHeaderVideo: Boolean
                get() = this is VideoComponent &&
                    when (size.width) {
                        is SizeConstraint.Fill -> true
                        is SizeConstraint.Fit,
                        is SizeConstraint.Fixed,
                        -> false
                    }
        }

        val windowInsetsState = WindowInsetsState()

        /**
         * Whether the current component should apply the top window insets.
         */
        val applyTopWindowInsets by windowInsetsState::applyTopWindowInsets

        /**
         * Whether the current component should ignore the top window insets.
         */
        val ignoreTopWindowInsets by windowInsetsState::ignoreTopWindowInsets

        var defaultTabIndex: Int? = null
        val rcPackage: Package?
            get() = packageInfo?.pkg
        val resolvedOffer: ResolvedOffer?
            get() = packageInfo?.resolvedOffer
        val offerEligibility: OfferEligibility?
            get() = packageInfo?.let { calculateOfferEligibility(it.resolvedOffer, it.pkg) }

        private val packagesOutsideTabs = mutableListOf<AvailablePackages.Info>()
        private val packagesByTab = mutableMapOf<Int, MutableList<AvailablePackages.Info>>()
        val packages: AvailablePackages
            get() = AvailablePackages(
                packagesOutsideTabs = packagesOutsideTabs,
                packagesByTab = packagesByTab,
            )

        /**
         * Temporarily changes the properties that influence a component's selected state, for the duration of [block].
         */
        fun <T> withSelectedScope(
            packageInfo: AvailablePackages.Info?,
            tabControlIndex: Int?,
            block: StyleFactoryScope.() -> T,
        ): T {
            if (packageInfo != null) recordPackage(packageInfo)

            val currentScope = copy()
            this.packageInfo = packageInfo
            this.tabControlIndex = tabControlIndex

            val result = block()

            this.packageInfo = currentScope.packageInfo
            this.tabControlIndex = currentScope.tabControlIndex

            return result
        }

        /**
         * Provides the [tabControl] to this branch of the tree.
         */
        fun <T> withTabControl(
            tabControl: TabControlStyle,
            block: StyleFactoryScope.() -> T,
        ): T {
            val currentScope = copy()
            this.tabControl = tabControl

            val result = block()

            this.tabControl = currentScope.tabControl

            return result
        }

        /**
         * Records that this branch of the tree is in a tab with the provided [tabIndex].
         */
        fun <T> withTabIndex(
            tabIndex: Int,
            block: StyleFactoryScope.() -> T,
        ): T {
            val currentScope = copy()
            this.tabIndex = tabIndex

            val result = block()

            this.tabIndex = currentScope.tabIndex

            return result
        }

        /**
         * Records that this branch of the tree is in a countdown with the provided [countdownDate] and [countFrom].
         */
        fun <T> withCountdown(
            countdownDate: Date,
            countFrom: CountdownComponent.CountFrom,
            block: StyleFactoryScope.() -> T,
        ): T {
            val currentScope = copy()
            this.countdownDate = countdownDate
            this.countFrom = countFrom

            val result = block()

            this.countdownDate = currentScope.countdownDate
            this.countFrom = currentScope.countFrom

            return result
        }

        inline fun <T> withCount(
            noinline predicate: (PaywallComponent) -> Boolean,
            block: StyleFactoryScope.() -> T,
        ): WithCount<T> {
            val maxKey = countPredicates.keys.maxOrNull() ?: -1
            val key = maxKey + 1
            countPredicates[key] = predicate
            countValues[key] = 0

            val value = block()
            val result = WithCount(value = value, count = countValues.getValue(key))

            countPredicates.remove(key)
            countValues.remove(key)

            return result
        }

        /**
         * Tells the StyleFactoryScope about a component. This should be called for every component in the tree.
         */
        fun recordComponent(component: PaywallComponent) {
            countPredicates.forEach { (key, predicate) ->
                if (predicate(component)) {
                    val currentValue = countValues[key] ?: 0
                    countValues[key] = currentValue + 1
                }
            }

            windowInsetsState.handleHeaderMediaViewWindowInsets(component)
        }

        /**
         * Applies the top window insets to the provided ComponentStyle if they haven't been applied to any other
         * component yet.
         */
        fun applyTopWindowInsetsIfNotYetApplied(to: ComponentStyle): ComponentStyle =
            when (to) {
                is StackComponentStyle -> to.copy(applyTopWindowInsets = !windowInsetsState.topWindowInsetsApplied)
                else -> to
            }

        /**
         * Applies the bottom window insets to this ComponentStyle if [shouldApply] is true and this is a stack or
         * sticky footer.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : ComponentStyle> T.applyBottomWindowInsetsIfNecessary(shouldApply: Boolean): T =
            if (shouldApply) {
                when (this) {
                    is StackComponentStyle -> copy(applyBottomWindowInsets = true)
                    is StickyFooterComponentStyle -> copy(
                        stackComponentStyle = stackComponentStyle.copy(applyBottomWindowInsets = true),
                    )

                    else -> this
                } as T
            } else {
                this
            }

        private fun recordPackage(pkg: AvailablePackages.Info) {
            val currentTabIndex = tabIndex
            if (currentTabIndex == null) {
                packagesOutsideTabs.add(pkg)
            } else {
                packagesByTab.getOrPut(currentTabIndex) { mutableListOf() }.add(pkg)
            }
        }
    }

    @Immutable
    class StyleResult(
        val componentStyle: ComponentStyle,
        val availablePackages: AvailablePackages,
        val defaultTabIndex: Int?,
    )

    /**
     * @param applyBottomWindowInsets Whether to apply bottom window insets to the root of this tree (i.e. the
     * passed-in [component]).
     */
    fun create(
        component: PaywallComponent,
        applyBottomWindowInsets: Boolean = false,
    ): Result<StyleResult, NonEmptyList<PaywallValidationError>> =
        with(StyleFactoryScope()) {
            createInternal(component)
                .flatMap { componentStyle ->
                    componentStyle?.let { Result.Success(it) }
                        ?: Result.Error(
                            nonEmptyListOf(PaywallValidationError.RootComponentUnsupportedProperties(component)),
                        )
                }
                .map { componentStyle -> applyTopWindowInsetsIfNotYetApplied(to = componentStyle) }
                .map { componentStyle -> componentStyle.applyBottomWindowInsetsIfNecessary(applyBottomWindowInsets) }
                .map { componentStyle ->
                    StyleResult(
                        componentStyle = componentStyle,
                        availablePackages = packages,
                        defaultTabIndex = defaultTabIndex,
                    )
                }
        }

    @Suppress("CyclomaticComplexMethod")
    private fun StyleFactoryScope.createInternal(
        component: PaywallComponent,
    ): Result<ComponentStyle?, NonEmptyList<PaywallValidationError>> {
        recordComponent(component)
        return when (component) {
            is ButtonComponent -> createButtonComponentStyleOrNull(component)
            is ImageComponent -> createImageComponentStyle(component)
            is PackageComponent -> createPackageComponentStyle(component)
            is PurchaseButtonComponent -> createPurchaseButtonComponentStyle(component)
            is StackComponent -> createStackComponentStyle(component)
            is StickyFooterComponent -> createStickyFooterComponentStyle(component)
            is TextComponent -> createTextComponentStyle(component)
            is IconComponent -> createIconComponentStyle(component)
            is TimelineComponent -> createTimelineComponentStyle(component)
            is CarouselComponent -> createCarouselComponentStyle(component)
            is TabControlButtonComponent -> createTabControlButtonComponentStyle(component)
            is TabControlToggleComponent -> createTabControlToggleComponentStyle(component)
            is TabControlComponent -> tabControl.errorIfNull(nonEmptyListOf(PaywallValidationError.TabControlNotInTab))
            is TabsComponent -> createTabsComponentStyle(component)
            is VideoComponent -> createVideoComponentStyle(component)
            is CountdownComponent -> createCountdownComponentStyle(
                component,
            )
        }
    }

    private fun StyleFactoryScope.createCountdownComponentStyle(
        component: CountdownComponent,
    ): Result<CountdownComponentStyle, NonEmptyList<PaywallValidationError>> =
        withCountdown(component.style.date, component.countFrom) {
            zipOrAccumulate(
                first = createStackComponentStyle(component.countdownStack),
                second = component.endStack?.let { createStackComponentStyle(it) }.orSuccessfullyNull(),
                third = component.fallback?.let { createStackComponentStyle(it) }.orSuccessfullyNull(),
            ) { countdownStack, endStack, fallbackStack ->
                CountdownComponentStyle(
                    date = component.style.date,
                    countFrom = component.countFrom,
                    countdownStackComponentStyle = countdownStack,
                    endStackComponentStyle = endStack,
                    fallbackStackComponentStyle = fallbackStack,
                )
            }
        }

    private fun StyleFactoryScope.createStickyFooterComponentStyle(
        component: StickyFooterComponent,
    ): Result<StickyFooterComponentStyle, NonEmptyList<PaywallValidationError>> =
        // tabControlIndex is null because a sticky footer cannot be _inside_ a tab control, which means we'll never
        // have to update the sticky footer based on the tab control being selected.
        withSelectedScope(packageInfo = null, tabControlIndex = null) {
            createStackComponentStyle(component.stack).map {
                StickyFooterComponentStyle(stackComponentStyle = it)
            }
        }

    private fun StyleFactoryScope.createButtonComponentStyleOrNull(
        component: ButtonComponent,
    ): Result<ButtonComponentStyle?, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = createStackComponentStyle(component.stack),
        second = convertAction(component.action),
    ) { stack, action ->
        action?.let {
            ButtonComponentStyle(
                stackComponentStyle = stack,
                action = action,
                transition = component.transition,
            )
        }
    }

    private fun StyleFactoryScope.createPackageComponentStyle(
        component: PackageComponent,
    ): Result<PackageComponentStyle?, NonEmptyList<PaywallValidationError>> =
        Result.Success(offering.getPackageOrNull(component.packageId))
            .flatMap { rcPackage ->
                if (rcPackage == null) {
                    val error = PaywallValidationError.MissingPackage(
                        offeringId = offering.identifier,
                        missingPackageId = component.packageId,
                        allPackageIds = offering.availablePackages.map { it.identifier },
                    )
                    Logger.w(error.message)
                    return Result.Success(null)
                }

                // Resolve Play Store offer if configured
                val resolvedOffer = PromoOfferResolver.resolve(
                    rcPackage = rcPackage,
                    offerConfig = component.playStoreOffer,
                )

                withSelectedScope(
                    packageInfo = AvailablePackages.Info(
                        pkg = rcPackage,
                        isSelectedByDefault = component.isSelectedByDefault,
                        resolvedOffer = resolvedOffer,
                    ),
                    // If a tab control contains a package, which is already an edge case, the package should not
                    // visually become "selected" if its tab control parent is.
                    tabControlIndex = null,
                ) {
                    val (stackComponentStyleResult, purchaseButtons) = withCount(
                        predicate = { it is PurchaseButtonComponent },
                    ) {
                        createStackComponentStyle(component.stack)
                    }

                    stackComponentStyleResult.map { stack ->
                        PackageComponentStyle(
                            stackComponentStyle = stack,
                            rcPackage = rcPackage,
                            isSelectedByDefault = component.isSelectedByDefault,
                            isSelectable = purchaseButtons == 0,
                            resolvedOffer = resolvedOffer,
                        )
                    }
                }
            }

    private fun StyleFactoryScope.createPurchaseButtonComponentStyle(
        component: PurchaseButtonComponent,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = createStackComponentStyle(component.stack),
        second = convertPurchaseButtonMethod(component.method ?: component.action?.toMethod()),
    ) { stack, action ->
        ButtonComponentStyle(
            stackComponentStyle = stack,
            action = action,
        )
    }

    @Suppress("MaxLineLength")
    private fun StyleFactoryScope.convertAction(
        action: ButtonComponent.Action,
    ): Result<ButtonComponentStyle.Action?, NonEmptyList<PaywallValidationError>> {
        return when (action) {
            is ButtonComponent.Action.NavigateBack -> Result.Success(ButtonComponentStyle.Action.NavigateBack)
            is ButtonComponent.Action.RestorePurchases -> Result.Success(ButtonComponentStyle.Action.RestorePurchases)
            is ButtonComponent.Action.NavigateTo -> convertDestination(action.destination)
                .map { destination -> destination?.let { ButtonComponentStyle.Action.NavigateTo(it) } }
            // Returning null here, which will result in this button being hidden.
            is ButtonComponent.Action.Unknown -> Result.Success(null)
        }
    }

    private fun StyleFactoryScope.convertPurchaseButtonMethod(
        method: PurchaseButtonComponent.Method?,
    ): Result<ButtonComponentStyle.Action, NonEmptyList<PaywallValidationError>> {
        if (method == null) {
            return Result.Success(
                ButtonComponentStyle.Action.PurchasePackage(
                    rcPackage = rcPackage,
                    resolvedOffer = resolvedOffer,
                ),
            )
        }
        return when (method) {
            is PurchaseButtonComponent.Method.InAppCheckout -> Result.Success(
                ButtonComponentStyle.Action.PurchasePackage(
                    rcPackage = rcPackage,
                    resolvedOffer = resolvedOffer,
                ),
            )

            is PurchaseButtonComponent.Method.WebCheckout -> {
                Result.Success(
                    ButtonComponentStyle.Action.WebCheckout(
                        rcPackage = rcPackage,
                        autoDismiss = method.autoDismiss ?: true,
                        openMethod = method.openMethod ?: ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                    ),
                )
            }

            is PurchaseButtonComponent.Method.WebProductSelection -> {
                Result.Success(
                    ButtonComponentStyle.Action.WebProductSelection(
                        autoDismiss = method.autoDismiss ?: true,
                        openMethod = method.openMethod ?: ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                    ),
                )
            }

            is PurchaseButtonComponent.Method.CustomWebCheckout -> localizations.stringForAllLocales(
                method.customUrl.urlLid,
            ).map { urls ->
                ButtonComponentStyle.Action.CustomWebCheckout(
                    urls = urls,
                    autoDismiss = method.autoDismiss ?: true,
                    openMethod = method.openMethod ?: ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                    rcPackage = rcPackage,
                    packageParam = method.customUrl.packageParam,
                )
            }

            is PurchaseButtonComponent.Method.Unknown -> {
                Logger.e("Unknown purchase button method. Defaulting to purchasing current/default package.")
                Result.Success(
                    ButtonComponentStyle.Action.PurchasePackage(
                        rcPackage = rcPackage,
                        resolvedOffer = resolvedOffer,
                    ),
                )
            }
        }
    }

    @Suppress("MaxLineLength")
    private fun StyleFactoryScope.convertDestination(
        destination: ButtonComponent.Destination,
    ): Result<ButtonComponentStyle.Action.NavigateTo.Destination?, NonEmptyList<PaywallValidationError>> =

        when (destination) {
            is ButtonComponent.Destination.CustomerCenter -> Result.Success(
                ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter,
            )

            is ButtonComponent.Destination.PrivacyPolicy -> buttonComponentStyleUrlDestination(
                destination.urlLid,
                destination.method,
            )
            is ButtonComponent.Destination.Terms -> buttonComponentStyleUrlDestination(
                destination.urlLid,
                destination.method,
            )
            is ButtonComponent.Destination.Url -> buttonComponentStyleUrlDestination(
                destination.urlLid,
                destination.method,
            )
            is ButtonComponent.Destination.Sheet ->
                createStackComponentStyle(destination.stack)
                    .map { it.applyBottomWindowInsetsIfNecessary(shouldApply = true) }
                    .map { stackComponentStyle ->
                        ButtonComponentStyle.Action.NavigateTo.Destination.Sheet(
                            id = destination.id,
                            name = destination.name,
                            stack = stackComponentStyle,
                            backgroundBlur = destination.backgroundBlur,
                            size = destination.size,
                        )
                    }
            // Returning null here, which will result in this button being hidden.
            is ButtonComponent.Destination.Unknown,
            -> Result.Success(null)
        }

    private fun buttonComponentStyleUrlDestination(
        urlLid: LocalizationKey,
        method: ButtonComponent.UrlMethod,
    ) =
        localizations.stringForAllLocales(urlLid).map { urls ->
            ButtonComponentStyle.Action.NavigateTo.Destination.Url(urls, method)
        }.map { urlDestination ->
            when (urlDestination.method) {
                ButtonComponent.UrlMethod.IN_APP_BROWSER,
                ButtonComponent.UrlMethod.EXTERNAL_BROWSER,
                ButtonComponent.UrlMethod.DEEP_LINK,
                -> urlDestination
                // Returning null here, which will result in this button being hidden.
                ButtonComponent.UrlMethod.UNKNOWN -> null
            }
        }

    @Suppress("CyclomaticComplexMethod")
    private fun StyleFactoryScope.createStackComponentStyle(
        component: StackComponent,
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            .toPresentedOverrides { partial ->
                PresentedStackPartial(
                    from = partial,
                    aliases = colorAliases,
                    createBadgeStackComponentStyle = { stackComponent -> createStackComponentStyle(stackComponent) },
                )
            }
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { createInternal(it) }
            .mapOrAccumulate { it }
            .map { it.filterNotNull() },
        third = component.badge
            ?.toBadgeStyle(createStackComponentStyle = { stackComponent -> createStackComponentStyle(stackComponent) })
            .orSuccessfullyNull(),
        fourth = createBackgroundStyles(component.background, component.backgroundColor),
        fifth = component.border?.toBorderStyles(colorAliases).orSuccessfullyNull(),
        sixth = component.shadow?.toShadowStyles(colorAliases).orSuccessfullyNull(),
    ) { presentedOverrides, children, badge, background, borderStyles, shadowStyles ->
        StackComponentStyle(
            children = children,
            dimension = component.dimension,
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            spacing = (component.spacing ?: DEFAULT_SPACING).dp,
            background = background,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.shape ?: DEFAULT_SHAPE,
            border = borderStyles,
            shadow = shadowStyles,
            badge = badge,
            scrollOrientation = component.overflow?.toOrientation(component.dimension),
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            countdownDate = countdownDate,
            countFrom = countFrom,
            overrides = presentedOverrides,
            applyTopWindowInsets = applyTopWindowInsets,
        )
    }

    private fun StyleFactoryScope.createTextComponentStyle(
        component: TextComponent,
    ): Result<TextComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Get our texts from the localization dictionary.
        first = localizations.stringForAllLocales(component.text)
            .flatMapError { errors ->
                val lidExistsInAnyLocale = localizations.any { (_, dict) -> dict.containsKey(component.text) }
                // If the lid exists in some locales but not all, it's a real localization/translation
                // issue, so we propagate the error. If it exists in NO locales, it's an orphan text_lid
                // from a frontend bug (e.g. a badge added only in an override state).
                if (lidExistsInAnyLocale) {
                    Result.Error(errors)
                } else {
                    Logger.w("Missing text for text_lid '${component.text.value}', using empty string.")
                    Result.Success(localizations.mapValues { "" })
                }
            },
        second = component.overrides
            // Map all overrides to PresentedOverrides.
            .toPresentedOverrides {
                LocalizedTextPartial(
                    from = it,
                    using = localizations,
                    aliases = colorAliases,
                    fontAliases = fontAliases,
                )
            }
            .mapError { nonEmptyListOf(it) },
        third = component.color.toColorStyles(colorAliases),
        fourth = component.backgroundColor?.toColorStyles(colorAliases).orSuccessfullyNull(),
        fifth = component.fontName
            ?.let { fontAlias -> fontAliases.getFontSpec(fontAlias) }
            ?.recoverFromFontAliasError()
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
    ) { texts, presentedOverrides, color, backgroundColor, fontSpec ->
        val weight = component.fontWeightInt?.let { FontWeight(it) }
            ?: component.fontWeight.toFontWeight()
        TextComponentStyle(
            texts = texts,
            color = color,
            fontSize = component.fontSize,
            fontWeight = weight,
            fontSpec = fontSpec,
            textAlign = component.horizontalAlignment.toTextAlign(),
            horizontalAlignment = component.horizontalAlignment.toAlignment(),
            backgroundColor = backgroundColor,
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            countdownDate = countdownDate,
            countFrom = countFrom,
            variableLocalizations = variableLocalizations,
            overrides = presentedOverrides,
        )
    }

    private fun StyleFactoryScope.createImageComponentStyle(
        component: ImageComponent,
    ): Result<ImageComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.source.withLocalizedOverrides(component.overrideSourceLid),
        second = component.overrides
            .toPresentedOverrides {
                it.source
                    ?.withLocalizedOverrides(it.overrideSourceLid)
                    .orSuccessfullyNull()
                    .flatMap { sources ->
                        PresentedImagePartial(from = it, sources = sources, aliases = colorAliases)
                    }
            }
            .mapError { nonEmptyListOf(it) },
        third = component.colorOverlay?.toColorStyles(aliases = colorAliases).orSuccessfullyNull(),
        fourth = component.border?.toBorderStyles(aliases = colorAliases).orSuccessfullyNull(),
        fifth = component.shadow?.toShadowStyles(aliases = colorAliases).orSuccessfullyNull(),
    ) { sources, presentedOverrides, overlay, border, shadow ->
        ImageComponentStyle(
            sources,
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.maskShape?.toShape(),
            border = border,
            shadow = shadow,
            overlay = overlay,
            contentScale = component.fitMode.toContentScale(),
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            overrides = presentedOverrides,
            ignoreTopWindowInsets = ignoreTopWindowInsets,
        )
    }

    private fun StyleFactoryScope.createVideoComponentStyle(
        component: VideoComponent,
    ): Result<VideoComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.source.withLocalizedOverrides(component.overrideSourceLid),
        second = component.fallbackSource?.withLocalizedOverrides(component.overrideSourceLid).orSuccessfullyNull(),
        third = component.overrides?.toPresentedOverrides { videoPartial ->
            videoPartial.source
                ?.withLocalizedOverrides(videoPartial.overrideSourceLid)
                .orSuccessfullyNull()
                .flatMap { sources ->
                    PresentedVideoPartial(
                        from = videoPartial,
                        sources = sources,
                        fallbackSources = videoPartial.fallbackSource
                            ?.withLocalizedOverrides(videoPartial.overrideSourceLid)
                            ?.let {
                                when (it) {
                                    is Result.Success -> it.value
                                    else -> null
                                }
                            },
                        aliases = colorAliases,
                    )
                }
        }
            ?.mapError { nonEmptyListOf(it) }
            .orSuccessfullyNull(),
        fourth = component.colorOverlay?.toColorStyles(aliases = colorAliases).orSuccessfullyNull(),
        fifth = component.border?.toBorderStyles(aliases = colorAliases).orSuccessfullyNull(),
        sixth = component.shadow?.toShadowStyles(aliases = colorAliases).orSuccessfullyNull(),
    ) { sources, fallbackSources, presentedOverrides, overlay, border, shadow ->
        VideoComponentStyle(
            sources = sources,
            fallbackSources = fallbackSources,
            overlay = overlay,
            border = border,
            shadow = shadow,
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            padding = component.padding?.toPaddingValues() ?: PaddingValues(),
            margin = component.margin?.toPaddingValues() ?: PaddingValues(),
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            overrides = presentedOverrides ?: emptyList(),
            showControls = component.showControls,
            autoplay = component.autoplay,
            loop = component.loop,
            muteAudio = component.muteAudio,
            shape = component.maskShape?.toShape(),
            contentScale = component.fitMode.toContentScale(),
            ignoreTopWindowInsets = ignoreTopWindowInsets,
        )
    }

    private fun StyleFactoryScope.createIconComponentStyle(
        component: IconComponent,
    ): Result<IconComponentStyle, NonEmptyList<PaywallValidationError>> =
        zipOrAccumulate(
            first = component.overrides
                .toPresentedOverrides { partial -> PresentedIconPartial(partial, colorAliases) }
                .mapError { nonEmptyListOf(it) },
            second = component.color
                ?.toColorStyles(aliases = colorAliases)
                .orSuccessfullyNull(),
            third = component.iconBackground
                ?.toBackground(aliases = colorAliases)
                .orSuccessfullyNull(),
        ) { presentedOverrides, colorStyles, background ->
            IconComponentStyle(
                baseUrl = component.baseUrl,
                iconName = component.iconName,
                formats = component.formats,
                visible = component.visible ?: DEFAULT_VISIBILITY,
                size = component.size,
                color = colorStyles,
                padding = component.padding.toPaddingValues(),
                margin = component.margin.toPaddingValues(),
                iconBackground = background,
                rcPackage = rcPackage,
                resolvedOffer = resolvedOffer,
                tabIndex = tabControlIndex,
                offerEligibility = offerEligibility,
                overrides = presentedOverrides,
            )
        }

    private fun StyleFactoryScope.createTimelineComponentStyle(
        component: TimelineComponent,
    ): Result<TimelineComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.overrides
            .toPresentedOverrides { partial -> Result.Success(PresentedTimelinePartial(partial)) }
            .mapError { nonEmptyListOf(it) },
        second = component.items
            .map { createTimelineComponentItemStyle(it) }
            .mapOrAccumulate { it },
    ) { presentedOverrides, items ->
        TimelineComponentStyle(
            itemSpacing = component.itemSpacing,
            textSpacing = component.textSpacing,
            columnGutter = component.columnGutter,
            iconAlignment = component.iconAlignment,
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            items = items,
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            overrides = presentedOverrides,
        )
    }

    private fun StyleFactoryScope.createTimelineComponentItemStyle(
        item: TimelineComponent.Item,
    ): Result<TimelineComponentStyle.ItemStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = item.overrides
            .toPresentedOverrides { partial -> PresentedTimelineItemPartial(partial, colorAliases) }
            .mapError { nonEmptyListOf(it) },
        second = createTextComponentStyle(item.title),
        third = item.description?.let { createTextComponentStyle(it) }.orSuccessfullyNull(),
        fourth = createIconComponentStyle(item.icon),
        fifth = item.connector?.color?.toColorStyles(colorAliases).orSuccessfullyNull(),
    ) { presentedOverrides, title, description, icon, connectorColor ->
        val connectorStyle = item.connector?.let { connector ->
            if (connectorColor != null) {
                TimelineComponentStyle.ConnectorStyle(
                    width = connector.width,
                    margin = connector.margin.toPaddingValues(),
                    color = connectorColor,
                )
            } else {
                null
            }
        }
        TimelineComponentStyle.ItemStyle(
            title = title,
            visible = item.visible ?: DEFAULT_VISIBILITY,
            description = description,
            icon = icon,
            connector = connectorStyle,
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            overrides = presentedOverrides,
        )
    }

    private fun StyleFactoryScope.createCarouselComponentStyle(
        component: CarouselComponent,
    ): Result<CarouselComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.overrides
            .toPresentedOverrides { partial -> PresentedCarouselPartial(partial, colorAliases) }
            .mapError { nonEmptyListOf(it) },
        second = component.pages
            .map { createStackComponentStyle(it) }
            .mapOrAccumulate { it },
        third = component.border?.toBorderStyles(colorAliases).orSuccessfullyNull(),
        fourth = component.shadow?.toShadowStyles(colorAliases).orSuccessfullyNull(),
        fifth = createBackgroundStyles(component.background, component.backgroundColor),
        sixth = component.pageControl?.toPageControlStyles(colorAliases).orSuccessfullyNull(),
    ) { presentedOverrides, stackComponentStyles, borderStyles, shadowStyles, background, pageControlStyles ->
        CarouselComponentStyle(
            pages = stackComponentStyles,
            initialPageIndex = component.initialPageIndex ?: 0,
            pageAlignment = component.pageAlignment.toAlignment(),
            visible = component.visible ?: DEFAULT_VISIBILITY,
            size = component.size,
            pagePeek = component.pagePeek?.dp ?: 0.dp,
            pageSpacing = (component.pageSpacing ?: DEFAULT_SPACING).dp,
            background = background,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.shape ?: DEFAULT_SHAPE,
            border = borderStyles,
            shadow = shadowStyles,
            pageControl = pageControlStyles,
            loop = component.loop ?: false,
            autoAdvance = component.autoAdvance,
            rcPackage = rcPackage,
            resolvedOffer = resolvedOffer,
            tabIndex = tabControlIndex,
            offerEligibility = offerEligibility,
            overrides = presentedOverrides,
        )
    }

    private fun StyleFactoryScope.createTabControlButtonComponentStyle(
        component: TabControlButtonComponent,
    ): Result<TabControlButtonComponentStyle, NonEmptyList<PaywallValidationError>> =
        withSelectedScope(packageInfo = null, tabControlIndex = component.tabIndex) {
            // Button control doesn't have a default tab.
            defaultTabIndex = 0
            createStackComponentStyle(component.stack)
                .map { stack -> TabControlButtonComponentStyle(tabIndex = component.tabIndex, stack = stack) }
        }

    private fun StyleFactoryScope.createTabControlToggleComponentStyle(
        component: TabControlToggleComponent,
    ): Result<TabControlToggleComponentStyle, NonEmptyList<PaywallValidationError>> =
        zipOrAccumulate(
            first = component.thumbColorOn.toColorStyles(aliases = colorAliases),
            second = component.thumbColorOff.toColorStyles(aliases = colorAliases),
            third = component.trackColorOn.toColorStyles(aliases = colorAliases),
            fourth = component.trackColorOff.toColorStyles(aliases = colorAliases),
        ) { thumbColorOn, thumbColorOff, trackColorOn, trackColorOff ->
            defaultTabIndex = if (component.defaultValue) 1 else 0
            TabControlToggleComponentStyle(
                thumbColorOn = thumbColorOn,
                thumbColorOff = thumbColorOff,
                trackColorOn = trackColorOn,
                trackColorOff = trackColorOff,
            )
        }

    private fun StyleFactoryScope.createTabsComponentStyle(
        component: TabsComponent,
    ): Result<TabsComponentStyle, NonEmptyList<PaywallValidationError>> =
        createTabsComponentStyleTabControl(component.control).flatMap { control ->
            // Find the index of the defaultTabId.
            component.defaultTabId
                ?.takeUnless { it.isBlank() }
                ?.let { defaultTabId -> component.tabs.indexOfFirst { it.id == defaultTabId } }
                ?.takeUnless { it == -1 }
                ?.also { defaultTabIndex = it }

            zipOrAccumulate(
                first = component.overrides
                    .toPresentedOverrides { partial -> PresentedTabsPartial(from = partial, aliases = colorAliases) }
                    .mapError { nonEmptyListOf(it) },
                second = createTabsComponentStyleTabs(component.tabs, control),
                third = createBackgroundStyles(component.background, component.backgroundColor),
                fourth = component.border?.toBorderStyles(colorAliases).orSuccessfullyNull(),
                fifth = component.shadow?.toShadowStyles(colorAliases).orSuccessfullyNull(),
            ) { overrides, tabs, backgroundColor, border, shadow ->
                TabsComponentStyle(
                    visible = component.visible ?: DEFAULT_VISIBILITY,
                    size = component.size,
                    padding = component.padding.toPaddingValues(),
                    margin = component.margin.toPaddingValues(),
                    background = backgroundColor,
                    shape = component.shape ?: DEFAULT_SHAPE,
                    border = border,
                    shadow = shadow,
                    control = control,
                    tabs = tabs,
                    overrides = overrides,
                )
            }
        }

    private fun StyleFactoryScope.createTabsComponentStyleTabControl(
        componentControl: TabsComponent.TabControl,
    ): Result<TabControlStyle, NonEmptyList<PaywallValidationError>> =
        withSelectedScope(packageInfo = null, tabControlIndex = null) {
            when (componentControl) {
                // This stack will contain a TabControlButtonComponent component.
                is TabsComponent.TabControl.Buttons -> createStackComponentStyle(
                    component = componentControl.stack,
                ).map { TabControlStyle.Buttons(it) }
                // This stack will contain a TabControlToggleComponent component.
                is TabsComponent.TabControl.Toggle -> createStackComponentStyle(
                    component = componentControl.stack,
                ).map { TabControlStyle.Toggle(it) }
            }
        }

    private fun StyleFactoryScope.createTabsComponentStyleTabs(
        componentTabs: List<TabsComponent.Tab>,
        control: TabControlStyle,
    ): Result<NonEmptyList<TabsComponentStyle.Tab>, NonEmptyList<PaywallValidationError>> =
        componentTabs
            .toNonEmptyListOrNull()
            .errorIfNull(nonEmptyListOf(PaywallValidationError.TabsComponentWithoutTabs))
            .flatMap { tabs ->
                tabs.mapIndexed { index, tab -> createTabsComponentStyleTab(tab, control, index) }.flatten()
            }

    private fun StyleFactoryScope.createTabsComponentStyleTab(
        componentTab: TabsComponent.Tab,
        control: TabControlStyle,
        tabIndex: Int,
    ): Result<TabsComponentStyle.Tab, NonEmptyList<PaywallValidationError>> =
        // We should only set the tabControlIndex for children of tab control components, not for all children of tab
        // components like this one.
        withSelectedScope(packageInfo = null, tabControlIndex = null) {
            withTabIndex(tabIndex) {
                withTabControl(control) {
                    createStackComponentStyle(componentTab.stack)
                        .map { stack -> TabsComponentStyle.Tab(stack) }
                }
            }
        }

    private fun createBackgroundStyles(
        background: Background?,
        backgroundColor: ColorScheme?,
    ): Result<BackgroundStyles?, NonEmptyList<PaywallValidationError>> =
        background?.toBackgroundStyles(colorAliases)
            ?: (
                backgroundColor?.toColorStyles(colorAliases)?.map { color -> BackgroundStyles.Color(color) }
                    .orSuccessfullyNull()
                )

    private fun ThemeImageUrls.withLocalizedOverrides(
        overrideSourceLid: LocalizationKey?,
    ): Result<NonEmptyMap<LocaleId, ThemeImageUrls>, NonEmptyList<PaywallValidationError.MissingImageLocalization>> =
        overrideSourceLid
            ?.let { key -> localizations.imageForAllLocales(key) }
            .orSuccessfullyNull()
            // Ensure the default source keyed by the default locale is present in the result.
            .map { nonEmptyMapOf(localizations.entry.key to this, it.orEmpty()) }

    private fun ThemeVideoUrls.withLocalizedOverrides(
        overrideSourceLid: LocalizationKey?,
    ): Result<NonEmptyMap<LocaleId, ThemeVideoUrls>, NonEmptyList<PaywallValidationError.MissingVideoLocalization>> =
        overrideSourceLid
            ?.let { key -> localizations.videoForAllLocales(key) }
            .orSuccessfullyNull()
            // Ensure the default source keyed by the default locale is present in the result.
            .map { nonEmptyMapOf(localizations.entry.key to this, it.orEmpty()) }

    private fun Offering.getPackageOrNull(identifier: String): Package? =
        try {
            getPackage(identifier)
        } catch (_: NoSuchElementException) {
            null
        }
}
