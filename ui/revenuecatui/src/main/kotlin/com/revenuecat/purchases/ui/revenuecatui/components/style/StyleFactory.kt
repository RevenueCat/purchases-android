package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
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
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedCarouselPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTabsPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelineItemPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedTimelinePartial
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.imageForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.stringForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.getFontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.toOrientation
import com.revenuecat.purchases.ui.revenuecatui.extensions.toPageControlStyles
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorIfNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatten
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptyListOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

@Suppress("TooManyFunctions")
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

    @Suppress("CyclomaticComplexMethod")
    fun create(
        component: PaywallComponent,
        rcPackage: Package? = null,
        tabControl: TabControlStyle? = null,
        tabIndex: Int? = null,
    ): Result<ComponentStyle, NonEmptyList<PaywallValidationError>> =
        when (component) {
            is ButtonComponent -> createButtonComponentStyle(component, rcPackage, tabControl, tabIndex)
            is ImageComponent -> createImageComponentStyle(component, rcPackage, tabIndex)
            is PackageComponent -> createPackageComponentStyle(component, tabControl)
            is PurchaseButtonComponent -> createPurchaseButtonComponentStyle(component, rcPackage, tabControl, tabIndex)
            is StackComponent -> createStackComponentStyle(component, rcPackage, tabControl, tabIndex)
            is StickyFooterComponent -> createStickyFooterComponentStyle(component, tabControl)
            is TextComponent -> createTextComponentStyle(component, rcPackage, tabIndex)
            is IconComponent -> createIconComponentStyle(component, rcPackage, tabIndex)
            is TimelineComponent -> createTimelineComponentStyle(component, rcPackage, tabIndex)
            is CarouselComponent -> createCarouselComponentStyle(component, rcPackage, tabControl, tabIndex)
            is TabControlButtonComponent -> createTabControlButtonComponentStyle(component)
            is TabControlToggleComponent -> createTabControlToggleComponentStyle(component)
            is TabControlComponent -> tabControl.errorIfNull(nonEmptyListOf(PaywallValidationError.TabControlNotInTab))
            is TabsComponent -> createTabsComponentStyle(component)
        }

    private fun createStickyFooterComponentStyle(
        component: StickyFooterComponent,
        tabControl: TabControlStyle?,
    ): Result<StickyFooterComponentStyle, NonEmptyList<PaywallValidationError>> =
        // tabIndex is null because a sticky footer cannot be _inside_ a tab control, which means we'll never have to
        // update the sticky footer based on the tab control being selected.
        createStackComponentStyle(component.stack, rcPackage = null, tabControl = tabControl, tabIndex = null).map {
            StickyFooterComponentStyle(stackComponentStyle = it)
        }

    private fun createButtonComponentStyle(
        component: ButtonComponent,
        rcPackage: Package?,
        tabControl: TabControlStyle?,
        tabIndex: Int?,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = createStackComponentStyle(component.stack, rcPackage, tabControl, tabIndex),
        second = component.action.toButtonComponentStyleAction(),
    ) { stack, action ->
        ButtonComponentStyle(
            stackComponentStyle = stack,
            action = action,
        )
    }

    private fun createPackageComponentStyle(
        component: PackageComponent,
        tabControl: TabControlStyle?,
    ): Result<PackageComponentStyle, NonEmptyList<PaywallValidationError>> =
        offering.getPackageOrNull(component.packageId)
            .errorIfNull(
                nonEmptyListOf(
                    PaywallValidationError.MissingPackage(
                        offeringId = offering.identifier,
                        missingPackageId = component.packageId,
                        allPackageIds = offering.availablePackages.map { it.identifier },
                    ),
                ),
            ).flatMap { rcPackage ->
                createStackComponentStyle(
                    component = component.stack,
                    rcPackage = rcPackage,
                    tabControl = tabControl,
                    // If a tab control contains a package, which is already an edge case, the package should not
                    // visually become "selected" if its tab control parent is.
                    tabIndex = null,
                ).map { stack ->
                    PackageComponentStyle(
                        stackComponentStyle = stack,
                        rcPackage = rcPackage,
                        isSelectedByDefault = component.isSelectedByDefault,
                    )
                }
            }

    private fun createPurchaseButtonComponentStyle(
        component: PurchaseButtonComponent,
        rcPackage: Package?,
        tabControl: TabControlStyle?,
        tabIndex: Int?,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
        rcPackage,
        tabControl,
        tabIndex,
    ).map {
        ButtonComponentStyle(
            stackComponentStyle = it,
            action = ButtonComponentStyle.Action.PurchasePackage,
        )
    }

    @Suppress("MaxLineLength")
    private fun ButtonComponent.Action.toButtonComponentStyleAction(): Result<ButtonComponentStyle.Action, NonEmptyList<PaywallValidationError>> {
        return when (this) {
            ButtonComponent.Action.NavigateBack -> Result.Success(ButtonComponentStyle.Action.NavigateBack)
            ButtonComponent.Action.RestorePurchases -> Result.Success(ButtonComponentStyle.Action.RestorePurchases)
            is ButtonComponent.Action.NavigateTo -> destination.toPaywallDestination()
                .map { ButtonComponentStyle.Action.NavigateTo(it) }
        }
    }

    @Suppress("MaxLineLength")
    private fun ButtonComponent.Destination.toPaywallDestination(): Result<ButtonComponentStyle.Action.NavigateTo.Destination, NonEmptyList<PaywallValidationError>> =

        when (this) {
            is ButtonComponent.Destination.CustomerCenter -> Result.Success(
                ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter,
            )

            is ButtonComponent.Destination.PrivacyPolicy -> buttonComponentStyleUrlDestination(urlLid, method)
            is ButtonComponent.Destination.Terms -> buttonComponentStyleUrlDestination(urlLid, method)
            is ButtonComponent.Destination.Url -> buttonComponentStyleUrlDestination(urlLid, method)
        }

    private fun buttonComponentStyleUrlDestination(
        urlLid: LocalizationKey,
        method: ButtonComponent.UrlMethod,
    ) =
        localizations.stringForAllLocales(urlLid).map { urls ->
            ButtonComponentStyle.Action.NavigateTo.Destination.Url(urls, method)
        }

    @Suppress("CyclomaticComplexMethod")
    private fun createStackComponentStyle(
        component: StackComponent,
        rcPackage: Package?,
        tabControl: TabControlStyle?,
        tabIndex: Int?,
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            .toPresentedOverrides { partial -> PresentedStackPartial(from = partial, aliases = colorAliases) }
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { create(it, rcPackage, tabControl, tabIndex) }
            .mapOrAccumulate { it },
        third = component.badge?.let { badge ->
            createStackComponentStyle(badge.stack, rcPackage, tabControl, tabIndex)
                .map {
                    BadgeStyle(
                        stackStyle = it,
                        style = badge.style,
                        alignment = badge.alignment,
                    )
                }
        }.orSuccessfullyNull(),
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
            tabIndex = tabIndex,
            overrides = presentedOverrides,
        )
    }

    private fun createTextComponentStyle(
        component: TextComponent,
        rcPackage: Package?,
        tabIndex: Int?,
    ): Result<TextComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Get our texts from the localization dictionary.
        first = localizations.stringForAllLocales(component.text),
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
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
    ) { texts, presentedOverrides, color, backgroundColor, fontSpec ->
        val weight = component.fontWeight.toFontWeight()
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
            tabIndex = tabIndex,
            variableLocalizations = variableLocalizations,
            overrides = presentedOverrides,
        )
    }

    private fun createImageComponentStyle(
        component: ImageComponent,
        rcPackage: Package?,
        tabIndex: Int?,
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
            tabIndex = tabIndex,
            overrides = presentedOverrides,
        )
    }

    private fun createIconComponentStyle(
        component: IconComponent,
        rcPackage: Package?,
        tabIndex: Int?,
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
                tabIndex = tabIndex,
                overrides = presentedOverrides,
            )
        }

    private fun createTimelineComponentStyle(
        component: TimelineComponent,
        rcPackage: Package?,
        tabIndex: Int?,
    ): Result<TimelineComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.overrides
            .toPresentedOverrides { partial -> Result.Success(PresentedTimelinePartial(partial)) }
            .mapError { nonEmptyListOf(it) },
        second = component.items
            .map { createTimelineComponentItemStyle(it, rcPackage, tabIndex) }
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
            tabIndex = tabIndex,
            overrides = presentedOverrides,
        )
    }

    private fun createTimelineComponentItemStyle(
        item: TimelineComponent.Item,
        rcPackage: Package?,
        tabIndex: Int?,
    ): Result<TimelineComponentStyle.ItemStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = item.overrides
            .toPresentedOverrides { partial -> PresentedTimelineItemPartial(partial, colorAliases) }
            .mapError { nonEmptyListOf(it) },
        second = createTextComponentStyle(item.title, rcPackage, tabIndex),
        third = item.description?.let { createTextComponentStyle(it, rcPackage, tabIndex) }.orSuccessfullyNull(),
        fourth = createIconComponentStyle(item.icon, rcPackage, tabIndex),
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
            tabIndex = tabIndex,
            overrides = presentedOverrides,
        )
    }

    private fun createCarouselComponentStyle(
        component: CarouselComponent,
        rcPackage: Package?,
        tabControl: TabControlStyle?,
        tabIndex: Int?,
    ): Result<CarouselComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.overrides
            .toPresentedOverrides { partial -> PresentedCarouselPartial(partial, colorAliases) }
            .mapError { nonEmptyListOf(it) },
        second = component.pages
            .map { createStackComponentStyle(it, rcPackage, tabControl, tabIndex) }
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
            tabIndex = tabIndex,
            overrides = presentedOverrides,
        )
    }

    private fun createTabControlButtonComponentStyle(
        component: TabControlButtonComponent,
    ): Result<TabControlButtonComponentStyle, NonEmptyList<PaywallValidationError>> =
        createStackComponentStyle(component.stack, rcPackage = null, tabControl = null, tabIndex = component.tabIndex)
            .map { stack -> TabControlButtonComponentStyle(tabIndex = component.tabIndex, stack = stack) }

    private fun createTabControlToggleComponentStyle(
        component: TabControlToggleComponent,
    ): Result<TabControlToggleComponentStyle, NonEmptyList<PaywallValidationError>> =
        zipOrAccumulate(
            first = component.thumbColorOn.toColorStyles(aliases = colorAliases),
            second = component.thumbColorOff.toColorStyles(aliases = colorAliases),
            third = component.trackColorOn.toColorStyles(aliases = colorAliases),
            fourth = component.trackColorOff.toColorStyles(aliases = colorAliases),
        ) { thumbColorOn, thumbColorOff, trackColorOn, trackColorOff ->
            TabControlToggleComponentStyle(
                defaultValue = component.defaultValue,
                thumbColorOn = thumbColorOn,
                thumbColorOff = thumbColorOff,
                trackColorOn = trackColorOn,
                trackColorOff = trackColorOff,
            )
        }

    private fun createTabsComponentStyle(
        component: TabsComponent,
    ): Result<TabsComponentStyle, NonEmptyList<PaywallValidationError>> =
        createTabsComponentStyleTabControl(component.control).flatMap { control ->
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

    private fun createTabsComponentStyleTabControl(
        componentControl: TabsComponent.TabControl,
    ): Result<TabControlStyle, NonEmptyList<PaywallValidationError>> =
        when (componentControl) {
            // This stack will contain a TabControlButtonComponent component.
            is TabsComponent.TabControl.Buttons -> createStackComponentStyle(
                component = componentControl.stack,
                rcPackage = null,
                tabControl = null,
                tabIndex = null,
            ).map { TabControlStyle.Buttons(it) }
            // This stack will contain a TabControlToggleComponent component.
            is TabsComponent.TabControl.Toggle -> createStackComponentStyle(
                component = componentControl.stack,
                rcPackage = null,
                tabControl = null,
                tabIndex = null,
            ).map { TabControlStyle.Toggle(it) }
        }

    private fun createTabsComponentStyleTabs(
        componentTabs: List<TabsComponent.Tab>,
        control: TabControlStyle,
    ): Result<NonEmptyList<TabsComponentStyle.Tab>, NonEmptyList<PaywallValidationError>> =
        componentTabs
            .toNonEmptyListOrNull()
            .errorIfNull(nonEmptyListOf(PaywallValidationError.TabsComponentWithoutTabs))
            .flatMap { tabs -> tabs.map { tab -> createTabsComponentStyleTab(tab, control) }.flatten() }

    private fun createTabsComponentStyleTab(
        componentTag: TabsComponent.Tab,
        control: TabControlStyle,
    ): Result<TabsComponentStyle.Tab, NonEmptyList<PaywallValidationError>> =
        // We should only set the tabIndex for children of tab control components, not for children of tab components
        // like this one.
        createStackComponentStyle(componentTag.stack, rcPackage = null, tabControl = control, tabIndex = null)
            .map { stack -> TabsComponentStyle.Tab(stack) }

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

    private fun Offering.getPackageOrNull(identifier: String): Package? =
        try {
            getPackage(identifier)
        } catch (_: NoSuchElementException) {
            null
        }
}
