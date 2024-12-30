@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.BadgeStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validate
import java.net.URL

@Suppress("LongMethod")
@Composable
internal fun StackComponentView(
    style: StackComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    // Get a StackComponentState that calculates the overridden properties we should use.
    val stackState = rememberUpdatedStackComponentState(
        style = style,
        paywallState = state,
        selected = selected,
    )

    if (stackState.visible) {
        if (style.badge != null) {
            when (style.badge.style) {
                Badge.Style.Overlay -> {
                    StackWithOverlaidBadge(stackState, state, style.badge.stackStyle, style.badge.alignment, modifier)
                }

                Badge.Style.EdgeToEdge -> {
                }

                Badge.Style.Nested -> {
                }
            }
        } else {
            MainStackComponent(stackState, state, modifier)
        }
    }
}

@Composable
private fun StackWithOverlaidBadge(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    badgeStack: StackComponentStyle,
    alignment: TwoDimensionalAlignment,
    modifier: Modifier = Modifier,
) {
    var badgeHeight by remember {
        mutableIntStateOf(0)
    }
    val paddingDp = with(LocalDensity.current) { (badgeHeight / 2f).toDp() }
    val padding = when (alignment) {
        TwoDimensionalAlignment.TOP_LEADING,
        TwoDimensionalAlignment.TOP,
        TwoDimensionalAlignment.TOP_TRAILING,
        -> PaddingValues(top = paddingDp)
        TwoDimensionalAlignment.BOTTOM_LEADING,
        TwoDimensionalAlignment.BOTTOM,
        TwoDimensionalAlignment.BOTTOM_TRAILING,
        -> PaddingValues(bottom = paddingDp)
        else -> PaddingValues(0.dp)
    }
    Box(modifier = modifier) {
        // TODO Fix margins when using badges
        MainStackComponent(stackState, state, modifier = Modifier.padding(padding))
        StackComponentView(
            badgeStack,
            state,
            modifier = Modifier
                .align(alignment.toAlignment())
                .onGloballyPositioned {
                    badgeHeight = it.size.height
                },
        )
    }
}

@Composable
private fun MainStackComponent(
    stackState: StackComponentState,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val backgroundColorStyle = stackState.backgroundColor?.let { rememberColorStyle(scheme = it) }
    val borderStyle = stackState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = stackState.shadow?.let { rememberShadowStyle(shadow = it) }

        // Modifier irrespective of dimension.
        val commonModifier = remember(stackState, backgroundColorStyle, borderStyle, shadowStyle) {
            Modifier
                .padding(stackState.margin)
                .applyIfNotNull(shadowStyle) { shadow(it, stackState.shape) }
                .applyIfNotNull(backgroundColorStyle) { background(it, stackState.shape) }
                .clip(stackState.shape)
                .applyIfNotNull(borderStyle) { border(it, stackState.shape) }
                .padding(stackState.padding)
        }

        val content: @Composable () -> Unit = remember(stackState.children) {
            @Composable { stackState.children.forEach { child -> ComponentView(style = child, state = state) } }
        }

        // Show the right container composable depending on the dimension.
        when (val dimension = stackState.dimension) {
            is Dimension.Horizontal -> Row(
                modifier = modifier
                    .size(stackState.size, verticalAlignment = dimension.alignment.toAlignment())
                    .then(commonModifier),
                verticalAlignment = dimension.alignment.toAlignment(),
                horizontalArrangement = dimension.distribution.toHorizontalArrangement(
                    spacing = stackState.spacing,
                ),
            ) { content() }

            is Dimension.Vertical -> Column(
                modifier = modifier
                    .size(stackState.size, horizontalAlignment = dimension.alignment.toAlignment())
                    .then(commonModifier),
                verticalArrangement = dimension.distribution.toVerticalArrangement(
                    spacing = stackState.spacing,
                ),
                horizontalAlignment = dimension.alignment.toAlignment(),
            ) { content() }

            is Dimension.ZLayer -> Box(
                modifier = modifier
                    .size(
                        size = stackState.size,
                        horizontalAlignment = dimension.alignment.toHorizontalAlignmentOrNull(),
                        verticalAlignment = dimension.alignment.toVerticalAlignmentOrNull(),
                    )
                    .then(commonModifier),
                contentAlignment = dimension.alignment.toAlignment(),
            ) { content() }
        }
    }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_Vertical() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 10.0,
                    x = 0.0,
                    y = 3.0,
                ),
                badge = null,
                overrides = null,
            ),
            state = previewEmptyState(),
        )
    }
}

private class BadgeAlignmentProvider : PreviewParameterProvider<TwoDimensionalAlignment> {
    override val values = listOf(
        TwoDimensionalAlignment.TOP_LEADING,
        TwoDimensionalAlignment.TOP,
        TwoDimensionalAlignment.TOP_TRAILING,
        TwoDimensionalAlignment.BOTTOM_LEADING,
        TwoDimensionalAlignment.BOTTOM,
        TwoDimensionalAlignment.BOTTOM_TRAILING,
    ).asSequence()
}

@Preview
@Composable
private fun StackComponentView_Preview_Badge(
    @PreviewParameter(BadgeAlignmentProvider::class) alignment: TwoDimensionalAlignment,
) {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
                size = Size(width = SizeConstraint.Fixed(200u), height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                ),
                padding = PaddingValues(all = 12.dp),
                margin = PaddingValues(all = 0.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = null,
                badge = previewBadge(alignment),
                overrides = null,
            ),
            state = previewEmptyState(),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_Horizontal() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = previewChildren(),
                dimension = Dimension.Horizontal(alignment = VerticalAlignment.CENTER, distribution = START),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 30.0,
                    x = 0.0,
                    y = 5.0,
                ),
                badge = null,
                overrides = null,
            ),
            state = previewEmptyState(),
        )
    }
}

@Suppress("LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun StackComponentView_Preview_ZLayer() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                children = listOf(
                    TextComponentStyle(
                        texts = nonEmptyMapOf(LocaleId("en_US") to "Hello"),
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR.toFontWeight(),
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Yellow.toArgb()),
                            dark = ColorInfo.Hex(Color.Red.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                        margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0).toPaddingValues(),
                        overrides = null,
                    ),
                    TextComponentStyle(
                        texts = nonEmptyMapOf(LocaleId("en_US") to "World"),
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR.toFontWeight(),
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Blue.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
                        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
                        overrides = null,
                    ),
                ),
                dimension = Dimension.ZLayer(alignment = TwoDimensionalAlignment.BOTTOM_TRAILING),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                backgroundColor = ColorScheme(
                    light = ColorInfo.Hex(Color.Red.toArgb()),
                    dark = ColorInfo.Hex(Color.Yellow.toArgb()),
                ),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = Border(width = 2.0, color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb()))),
                shadow = Shadow(
                    color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
                    radius = 20.0,
                    x = 5.0,
                    y = 5.0,
                ),
                badge = null,
                overrides = null,
            ),
            state = previewEmptyState(),
        )
    }
}

@Composable
private fun previewChildren() = listOf(
    TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to "Hello"),
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR.toFontWeight(),
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
        overrides = null,
    ),
    TextComponentStyle(
        texts = nonEmptyMapOf(LocaleId("en_US") to "World"),
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR.toFontWeight(),
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER.toTextAlign(),
        horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0).toPaddingValues(),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0).toPaddingValues(),
        overrides = null,
    ),
)

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one StackComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )

    return offering.toComponentsPaywallState(data.validate().getOrThrow())
}

private fun previewBadge(alignment: TwoDimensionalAlignment): BadgeStyle {
    return BadgeStyle(
        stackStyle = StackComponentStyle(
            children = listOf(
                TextComponentStyle(
                    texts = nonEmptyMapOf(LocaleId("en_US") to "Badge"),
                    color = ColorScheme(
                        light = ColorInfo.Hex(Color.Black.toArgb()),
                    ),
                    fontSize = FontSize.BODY_M,
                    fontWeight = FontWeight.REGULAR.toFontWeight(),
                    fontFamily = null,
                    textAlign = HorizontalAlignment.CENTER.toTextAlign(),
                    horizontalAlignment = HorizontalAlignment.CENTER.toAlignment(),
                    backgroundColor = null,
                    size = Size(width = Fit, height = Fit),
                    padding = Padding(
                        top = 8.0,
                        bottom = 8.0,
                        leading = 8.0,
                        trailing = 8.0,
                    ).toPaddingValues(),
                    margin = Padding(
                        top = 0.0,
                        bottom = 0.0,
                        leading = 0.0,
                        trailing = 0.0,
                    ).toPaddingValues(),
                    overrides = null,
                ),
            ),
            dimension = Dimension.Horizontal(alignment = VerticalAlignment.CENTER, distribution = START),
            size = Size(width = Fit, height = Fit),
            spacing = 0.dp,
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(Color.Green.toArgb()),
            ),
            padding = PaddingValues(all = 0.dp),
            margin = PaddingValues(all = 0.dp),
            shape = CircleShape,
            border = null,
            shadow = null,
            badge = null,
            overrides = null,
        ),
        style = Badge.Style.Overlay,
        alignment = alignment,
    )
}
