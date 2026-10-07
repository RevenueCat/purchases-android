package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CarouselComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.FallbackHeaderComponent
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
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
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme

/**
 * A loading screen for the first step of a flow: the SDK has the paywall tree before it knows which
 * step the audiences pick, so it greys that tree out rather than showing a spinner.
 */
internal class WorkflowSkeleton private constructor(
    private val tone: ColorScheme,
    private val colors: Map<ColorAlias, ColorScheme>,
) {

    @OptIn(InternalRevenueCatAPI::class)
    private fun stack(
        stack: StackComponent,
        contentHidden: Boolean = false,
        forceBlock: Boolean = false,
    ): StackComponent {
        val isBlock = forceBlock || hasFill(stack.background, stack.backgroundColor, stack.border)
        return StackComponent(
            components = stack.components.mapNotNull { component(it, contentHidden || isBlock) },
            visible = stack.visible,
            dimension = stack.dimension,
            size = stack.size,
            spacing = stack.spacing,
            backgroundColor = if (isBlock && !contentHidden) tone else null,
            padding = stack.padding,
            margin = stack.margin,
            shape = stack.shape,
            border = stack.border?.let { Border(color = if (contentHidden) CLEAR else tone, width = it.width) },
            // Overrides change size and visibility by condition, so the skeleton keeps them.
            overrides = stack.overrides.map { override(it) { partial -> stackPartial(partial, contentHidden) } },
            // A badge adds to the measured size, so it stays, with its own stack as a skeleton too.
            badge = stack.badge?.let { Badge(this.stack(it.stack, contentHidden), it.style, it.alignment) },
            overflow = stack.overflow,
        )
    }

    /**
     * Invisible content still measures fit-sized blocks, without showing their labels.
     */
    @OptIn(InternalRevenueCatAPI::class)
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun component(component: PaywallComponent, contentHidden: Boolean): PaywallComponent? =
        when (component) {
            is TextComponent -> if (component.fontSize >= MIN_TEXT_FONT_SIZE || contentHidden) {
                TextComponent(
                    text = component.text,
                    color = if (contentHidden) CLEAR else tone,
                    visible = component.visible,
                    fontName = component.fontName,
                    fontWeight = component.fontWeight,
                    fontWeightInt = component.fontWeightInt,
                    fontSize = component.fontSize,
                    horizontalAlignment = component.horizontalAlignment,
                    size = component.size,
                    padding = component.padding,
                    margin = component.margin,
                    overrides = component.overrides.map { override(it) { p -> textPartial(p, contentHidden) } },
                )
            } else {
                null
            }
            is StackComponent -> stack(component, contentHidden)
            is ButtonComponent -> if (component.visible == false) null else stack(component.stack, contentHidden)
            is PackageComponent -> if (component.visible == false) {
                null
            } else {
                stack(component.stack, contentHidden, forceBlock = true)
            }
            is PurchaseButtonComponent -> stack(component.stack, contentHidden, forceBlock = true)
            is StickyFooterComponent -> stack(component.stack, contentHidden)
            is HeaderComponent -> stack(component.stack, contentHidden)
            is ImageComponent -> image(component, contentHidden)
            // The skeleton has no density, so it cannot turn the source's pixels into a height. The
            // fallback image goes through the normal image sizing instead. The video url is not an
            // image, so it never reaches the loader.
            is VideoComponent -> component.fallbackSource?.let { fallback ->
                image(
                    ImageComponent(
                        source = fallback,
                        visible = component.visible,
                        size = component.size,
                        maskShape = component.maskShape,
                        fitMode = component.fitMode,
                        padding = component.padding ?: PADDING_ZERO,
                        margin = component.margin ?: PADDING_ZERO,
                        border = component.border,
                    ),
                    contentHidden,
                )
            } ?: StackComponent(
                components = emptyList(),
                visible = component.visible,
                size = component.size,
                backgroundColor = if (contentHidden) null else tone,
                padding = component.padding ?: PADDING_ZERO,
                margin = component.margin ?: PADDING_ZERO,
                border = component.border?.let {
                    Border(color = if (contentHidden) CLEAR else tone, width = it.width)
                },
            )
            is TabsComponent -> stack(
                StackComponent(
                    components = (
                        component.tabs.firstOrNull { it.id == component.defaultTabId } ?: component.tabs.firstOrNull()
                        )?.let { listOf(it.stack) } ?: emptyList(),
                    visible = component.visible,
                    size = component.size,
                    backgroundColor = component.backgroundColor,
                    background = component.background,
                    padding = component.padding,
                    margin = component.margin,
                    shape = component.shape,
                    border = component.border,
                ),
                contentHidden,
            )
            is CarouselComponent -> stack(
                StackComponent(
                    components = component.pages.getOrNull(component.initialPageIndex ?: 0)
                        ?.let { listOf(it) } ?: emptyList(),
                    visible = component.visible,
                    size = component.size,
                    backgroundColor = component.backgroundColor,
                    background = component.background,
                    padding = component.padding,
                    margin = component.margin,
                    shape = component.shape,
                    border = component.border,
                ),
                contentHidden,
            )
            is CountdownComponent -> stack(component.countdownStack, contentHidden)
            is TimelineComponent -> stack(
                StackComponent(
                    components = component.items.map { item ->
                        StackComponent(
                            components = listOfNotNull(item.title, item.description),
                            spacing = component.textSpacing.toFloat(),
                        )
                    },
                    visible = component.visible,
                    size = component.size,
                    spacing = component.itemSpacing.toFloat(),
                    padding = component.padding,
                    margin = component.margin,
                ),
                contentHidden,
            )
            // These draw their own live content and have no meaningful grey skeleton.
            is IconComponent,
            is WebViewComponent,
            is TabControlComponent,
            is TabControlButtonComponent,
            is TabControlToggleComponent,
            FallbackHeaderComponent,
            -> null
        }

    @OptIn(InternalRevenueCatAPI::class)
    private fun <T : PartialComponent> override(
        source: ComponentOverride<T>,
        transform: (T) -> T,
    ): ComponentOverride<T> = ComponentOverride(source.conditions, transform(source.properties))

    @OptIn(InternalRevenueCatAPI::class)
    private fun stackPartial(partial: PartialStackComponent, contentHidden: Boolean) = PartialStackComponent(
        visible = partial.visible,
        dimension = partial.dimension,
        size = partial.size,
        spacing = partial.spacing,
        backgroundColor = partial.backgroundColor?.let { if (contentHidden) CLEAR else tone },
        padding = partial.padding,
        margin = partial.margin,
        shape = partial.shape,
        border = partial.border?.let { Border(color = if (contentHidden) CLEAR else tone, width = it.width) },
        overflow = partial.overflow,
    )

    @OptIn(InternalRevenueCatAPI::class)
    private fun textPartial(partial: PartialTextComponent, contentHidden: Boolean) = PartialTextComponent(
        visible = partial.visible,
        text = partial.text,
        color = partial.color?.let { if (contentHidden) CLEAR else tone },
        fontName = partial.fontName,
        fontWeight = partial.fontWeight,
        fontWeightInt = partial.fontWeightInt,
        fontSize = partial.fontSize,
        horizontalAlignment = partial.horizontalAlignment,
        size = partial.size,
        padding = partial.padding,
        margin = partial.margin,
    )

    @OptIn(InternalRevenueCatAPI::class)
    private fun imagePartial(partial: PartialImageComponent, contentHidden: Boolean) = PartialImageComponent(
        visible = partial.visible,
        source = partial.source,
        size = partial.size,
        overrideSourceLid = partial.overrideSourceLid,
        fitMode = partial.fitMode,
        maskShape = partial.maskShape,
        colorOverlay = tone,
        padding = partial.padding,
        margin = partial.margin,
        border = partial.border?.let { Border(color = if (contentHidden) CLEAR else tone, width = it.width) },
    )

    @OptIn(InternalRevenueCatAPI::class)
    private fun image(image: ImageComponent, contentHidden: Boolean): ImageComponent =
        ImageComponent(
            source = image.source,
            visible = image.visible,
            size = image.size,
            maskShape = image.maskShape,
            // An overlay paints over the bitmap, so a clear one would leave the real image showing.
            // Inside a hidden block the tone matches the block, which is what makes the image vanish.
            colorOverlay = tone,
            fitMode = image.fitMode,
            padding = image.padding,
            margin = image.margin,
            border = image.border?.let { Border(color = if (contentHidden) CLEAR else tone, width = it.width) },
            overrides = image.overrides.map { override(it) { p -> imagePartial(p, contentHidden) } },
        )

    @OptIn(InternalRevenueCatAPI::class)
    private fun hasFill(background: Background?, color: ColorScheme?, border: Border?): Boolean {
        val borderFills = border != null && border.width > 0 && isVisible(border.color)
        val colorFills = color != null && isVisible(color)
        val backgroundFills = when (background) {
            null -> false
            is Background.Color -> isVisible(background.value)
            else -> true
        }
        return borderFills || colorFills || backgroundFills
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun isVisible(color: ColorScheme): Boolean =
        alpha(color.light, colors, dark = false) > 0 || alpha(color.dark ?: color.light, colors, dark = true) > 0

    @OptIn(InternalRevenueCatAPI::class)
    private fun alpha(color: ColorInfo, colors: Map<ColorAlias, ColorScheme>, dark: Boolean): Int =
        argbValues(color, colors, dark).maxOfOrNull { (it ushr ALPHA_SHIFT) and COLOR_MASK } ?: 0

    internal companion object {

        private const val MIN_TEXT_FONT_SIZE = 14
        private const val COLOR_MASK = 0xFF
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val RED_WEIGHT = 0.299
        private const val GREEN_WEIGHT = 0.587
        private const val BLUE_WEIGHT = 0.114
        private const val MID_BRIGHTNESS = 0.5
        private const val DARK_FALLBACK_BRIGHTNESS = 0.08
        private const val LIGHT_FALLBACK_BRIGHTNESS = 1.0

        @OptIn(InternalRevenueCatAPI::class)
        private val CLEAR = ColorScheme(light = ColorInfo.Hex(0x00000000))

        @OptIn(InternalRevenueCatAPI::class)
        private val DARK_PLACEHOLDER = ColorInfo.Hex(0xFF383838.toInt())

        @OptIn(InternalRevenueCatAPI::class)
        private val LIGHT_PLACEHOLDER = ColorInfo.Hex(0xFFD8D8D8.toInt())

        @OptIn(InternalRevenueCatAPI::class)
        private val MEDIA_BACKGROUND_FALLBACK = ColorScheme(
            light = ColorInfo.Hex(0xFFFFFFFF.toInt()),
            dark = ColorInfo.Hex(0xFF151515.toInt()),
        )

        @OptIn(InternalRevenueCatAPI::class)
        private val PADDING_ZERO = com.revenuecat.purchases.paywalls.components.properties.Padding.zero

        @OptIn(InternalRevenueCatAPI::class)
        @JvmSynthetic
        fun transform(
            data: PaywallComponentsData,
            colors: Map<ColorAlias, ColorScheme> = emptyMap(),
        ): PaywallComponentsData {
            val base = data.componentsConfig.base
            val background = backgroundColor(base.background)
            val light = brightness(background.light, colors, dark = false)
            val dark = brightness(background.dark ?: background.light, colors, dark = true)
            val transform = WorkflowSkeleton(
                tone = ColorScheme(
                    light = if (light < MID_BRIGHTNESS) DARK_PLACEHOLDER else LIGHT_PLACEHOLDER,
                    dark = if (dark < MID_BRIGHTNESS) DARK_PLACEHOLDER else LIGHT_PLACEHOLDER,
                ),
                colors = colors,
            )
            return PaywallComponentsData(
                id = data.id,
                templateName = data.templateName,
                assetBaseURL = data.assetBaseURL,
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = transform.stack(base.stack),
                        background = Background.Color(background),
                        stickyFooter = base.stickyFooter?.let { StickyFooterComponent(transform.stack(it.stack)) },
                        header = base.header?.let {
                            HeaderComponent(transform.stack(it.stack))
                        },
                    ),
                ),
                componentsLocalizations = data.componentsLocalizations,
                defaultLocaleIdentifier = data.defaultLocaleIdentifier,
                revision = data.revision,
                zeroDecimalPlaceCountries = data.zeroDecimalPlaceCountries,
                exitOffers = null,
                productChangeConfig = data.productChangeConfig,
                automaticallyScaleFontSize = data.automaticallyScaleFontSize,
                stateDeclarations = data.stateDeclarations,
            )
        }

        @OptIn(InternalRevenueCatAPI::class)
        private fun backgroundColor(background: Background): ColorScheme =
            when (background) {
                is Background.Color -> background.value
                is Background.Image -> background.colorOverlay ?: MEDIA_BACKGROUND_FALLBACK
                is Background.Video -> background.colorOverlay ?: MEDIA_BACKGROUND_FALLBACK
                is Background.Unknown -> MEDIA_BACKGROUND_FALLBACK
            }

        @OptIn(InternalRevenueCatAPI::class)
        private fun argbValues(
            color: ColorInfo,
            colors: Map<ColorAlias, ColorScheme>,
            dark: Boolean,
        ): List<Int> =
            when (color) {
                is ColorInfo.Hex -> listOf(color.value)
                is ColorInfo.Gradient.Linear -> color.points.map { it.color }
                is ColorInfo.Gradient.Radial -> color.points.map { it.color }
                is ColorInfo.Alias -> colors[color.value]
                    ?.let { argbValues(if (dark) it.dark ?: it.light else it.light, emptyMap(), dark) }
                    ?: emptyList()
            }

        @OptIn(InternalRevenueCatAPI::class)
        private fun brightness(color: ColorInfo, colors: Map<ColorAlias, ColorScheme>, dark: Boolean): Double {
            val values = argbValues(color, colors, dark).map { argb ->
                (
                    RED_WEIGHT * ((argb ushr RED_SHIFT) and COLOR_MASK) +
                        GREEN_WEIGHT * ((argb ushr GREEN_SHIFT) and COLOR_MASK) +
                        BLUE_WEIGHT * (argb and COLOR_MASK)
                    ) / COLOR_MASK
            }
            return if (values.isEmpty()) {
                if (dark) DARK_FALLBACK_BRIGHTNESS else LIGHT_FALLBACK_BRIGHTNESS
            } else {
                values.average()
            }
        }
    }
}
