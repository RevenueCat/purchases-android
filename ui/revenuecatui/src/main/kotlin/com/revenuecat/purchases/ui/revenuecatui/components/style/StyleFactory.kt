package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
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
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedIconPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
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
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.getFontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorIfNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

@Suppress("TooManyFunctions")
internal class StyleFactory(
    private val localizations: NonEmptyMap<LocaleId, LocalizationDictionary>,
    private val colorAliases: Map<ColorAlias, ColorScheme>,
    private val fontAliases: Map<FontAlias, FontSpec>,
    private val variableLocalizations: NonEmptyMap<LocaleId, NonEmptyMap<VariableLocalizationKey, String>>,
    private val offering: Offering,
) {

    private companion object {
        private const val DEFAULT_SPACING = 0f
    }

    fun create(
        component: PaywallComponent,
        rcPackage: Package? = null,
    ): Result<ComponentStyle, NonEmptyList<PaywallValidationError>> =
        when (component) {
            is ButtonComponent -> createButtonComponentStyle(component, rcPackage)
            is ImageComponent -> createImageComponentStyle(component, rcPackage)
            is PackageComponent -> createPackageComponentStyle(component)
            is PurchaseButtonComponent -> createPurchaseButtonComponentStyle(component, rcPackage)
            is StackComponent -> createStackComponentStyle(component, rcPackage)
            is StickyFooterComponent -> createStickyFooterComponentStyle(component)
            is TextComponent -> createTextComponentStyle(component, rcPackage)
            is IconComponent -> createIconComponentStyle(component, rcPackage)
            is TimelineComponent -> createTimelineComponentStyle(component, rcPackage)
            is TabControlButtonComponent -> TODO()
            is TabControlComponent -> TODO()
            is TabControlToggleComponent -> TODO()
            is TabsComponent -> TODO()
        }

    private fun createStickyFooterComponentStyle(
        component: StickyFooterComponent,
    ): Result<StickyFooterComponentStyle, NonEmptyList<PaywallValidationError>> =
        createStackComponentStyle(component.stack, rcPackage = null).map {
            StickyFooterComponentStyle(stackComponentStyle = it)
        }

    private fun createButtonComponentStyle(
        component: ButtonComponent,
        rcPackage: Package?,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = createStackComponentStyle(component.stack, rcPackage),
        second = component.action.toButtonComponentStyleAction(),
    ) { stack, action ->
        ButtonComponentStyle(
            stackComponentStyle = stack,
            action = action,
        )
    }

    private fun createPackageComponentStyle(
        component: PackageComponent,
    ): Result<PackageComponentStyle, NonEmptyList<PaywallValidationError>> =
        offering.getPackage(component.packageId)
            .errorIfNull(
                nonEmptyListOf(
                    PaywallValidationError.MissingPackage(
                        offeringId = offering.identifier,
                        packageId = component.packageId,
                    ),
                ),
            ).flatMap { rcPackage ->
                createStackComponentStyle(
                    component = component.stack,
                    rcPackage = rcPackage,
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
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
        rcPackage,
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
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            ?.toPresentedOverrides { partial -> PresentedStackPartial(from = partial, aliases = colorAliases) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { create(it, rcPackage) }
            .mapOrAccumulate { it },
        third = component.badge?.let { badge ->
            createStackComponentStyle(badge.stack, rcPackage)
                .map {
                    BadgeStyle(
                        stackStyle = it,
                        style = badge.style,
                        alignment = badge.alignment,
                    )
                }
        }.orSuccessfullyNull(),
        fourth = component.backgroundColor?.toColorStyles(colorAliases).orSuccessfullyNull(),
        fifth = component.border?.toBorderStyles(colorAliases).orSuccessfullyNull(),
        sixth = component.shadow?.toShadowStyles(colorAliases).orSuccessfullyNull(),
    ) { presentedOverrides, children, badge, backgroundColorStyles, borderStyles, shadowStyles ->
        StackComponentStyle(
            children = children,
            dimension = component.dimension,
            size = component.size,
            spacing = (component.spacing ?: DEFAULT_SPACING).dp,
            backgroundColor = backgroundColorStyles,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.shape ?: Shape.Rectangle(),
            border = borderStyles,
            shadow = shadowStyles,
            badge = badge,
            rcPackage = rcPackage,
            overrides = presentedOverrides,
        )
    }

    private fun createTextComponentStyle(
        component: TextComponent,
        rcPackage: Package?,
    ): Result<TextComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Get our texts from the localization dictionary.
        first = localizations.stringForAllLocales(component.text),
        second = component.overrides
            // Map all overrides to PresentedOverrides.
            ?.toPresentedOverrides {
                LocalizedTextPartial(
                    from = it,
                    using = localizations,
                    aliases = colorAliases,
                    fontAliases = fontAliases,
                )
            }
            .orSuccessfullyNull()
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
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            rcPackage = rcPackage,
            variableLocalizations = variableLocalizations,
            overrides = presentedOverrides,
        )
    }

    private fun createImageComponentStyle(
        component: ImageComponent,
        rcPackage: Package?,
    ): Result<ImageComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.source.withLocalizedOverrides(component.overrideSourceLid),
        second = component.overrides
            ?.toPresentedOverrides {
                it.source
                    ?.withLocalizedOverrides(it.overrideSourceLid)
                    .orSuccessfullyNull()
                    .flatMap { sources ->
                        PresentedImagePartial(from = it, sources = sources, aliases = colorAliases)
                    }
            }.orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        third = component.colorOverlay?.toColorStyles(aliases = colorAliases).orSuccessfullyNull(),
        fourth = component.border?.toBorderStyles(aliases = colorAliases).orSuccessfullyNull(),
        fifth = component.shadow?.toShadowStyles(aliases = colorAliases).orSuccessfullyNull(),
    ) { sources, presentedOverrides, overlay, border, shadow ->
        ImageComponentStyle(
            sources,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.maskShape?.toShape(),
            border = border,
            shadow = shadow,
            overlay = overlay,
            contentScale = component.fitMode.toContentScale(),
            rcPackage = rcPackage,
            overrides = presentedOverrides,
        )
    }

    private fun createIconComponentStyle(
        component: IconComponent,
        rcPackage: Package?,
    ): Result<IconComponentStyle, NonEmptyList<PaywallValidationError>> =
        zipOrAccumulate(
            first = component.overrides
                ?.toPresentedOverrides { partial -> PresentedIconPartial(partial, colorAliases) }
                .orSuccessfullyNull()
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
                size = component.size,
                color = colorStyles,
                padding = component.padding.toPaddingValues(),
                margin = component.margin.toPaddingValues(),
                iconBackground = background,
                rcPackage = rcPackage,
                overrides = presentedOverrides,
            )
        }

    private fun createTimelineComponentStyle(
        component: TimelineComponent,
        rcPackage: Package?,
    ): Result<TimelineComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.overrides
            ?.toPresentedOverrides { partial -> Result.Success(PresentedTimelinePartial(partial)) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        second = component.items
            .map { createTimelineComponentItemStyle(it, rcPackage) }
            .mapOrAccumulate { it },
    ) { presentedOverrides, items ->
        TimelineComponentStyle(
            itemSpacing = component.itemSpacing,
            textSpacing = component.textSpacing,
            columnGutter = component.columnGutter,
            iconAlignment = component.iconAlignment,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            items = items,
            rcPackage = rcPackage,
            overrides = presentedOverrides,
        )
    }

    private fun createTimelineComponentItemStyle(
        item: TimelineComponent.Item,
        rcPackage: Package?,
    ): Result<TimelineComponentStyle.ItemStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = item.overrides
            ?.toPresentedOverrides { partial -> PresentedTimelineItemPartial(partial, colorAliases) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        second = createTextComponentStyle(item.title, rcPackage),
        third = item.description?.let { createTextComponentStyle(it, rcPackage) }.orSuccessfullyNull(),
        fourth = createIconComponentStyle(item.icon, rcPackage),
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
            description = description,
            icon = icon,
            connector = connectorStyle,
            rcPackage = rcPackage,
            overrides = presentedOverrides,
        )
    }

    private fun ThemeImageUrls.withLocalizedOverrides(
        overrideSourceLid: LocalizationKey?,
    ): Result<NonEmptyMap<LocaleId, ThemeImageUrls>, NonEmptyList<PaywallValidationError.MissingImageLocalization>> =
        overrideSourceLid
            ?.let { key -> localizations.imageForAllLocales(key) }
            .orSuccessfullyNull()
            // Ensure the default source keyed by the default locale is present in the result.
            .map { nonEmptyMapOf(localizations.entry.key to this, it.orEmpty()) }
}
