@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.image.ImageComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toHorizontalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalAlignmentOrNull
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toVerticalArrangement
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle.Solid
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

@Suppress("LongMethod")
@Composable
internal fun StackComponentView(
    style: StackComponentStyle,
    modifier: Modifier = Modifier,
) {
    if (style.visible) {
        // Modifier irrespective of dimension.
        val commonModifier = remember(style) {
            Modifier
                .padding(style.margin)
                .applyIfNotNull(style.shadow) { shadow(it, style.shape) }
                .applyIfNotNull(style.background) { background(it, style.shape) }
                .clip(style.shape)
                .applyIfNotNull(style.border) { border(it, style.shape) }
                .padding(style.padding)
        }

        val content: @Composable () -> Unit = remember(style.children) {
            @Composable {
                style.children.forEach { child ->
                    when (child) {
                        is StackComponentStyle -> StackComponentView(style = child)
                        is TextComponentStyle -> TextComponentView(style = child)
                        is ImageComponentStyle -> ImageComponentView(style = child)
                    }
                }
            }
        }

        // Show the right container composable depending on the dimension.
        when (style.dimension) {
            is Dimension.Horizontal -> Row(
                modifier = modifier
                    .size(style.size, verticalAlignment = style.dimension.alignment.toAlignment())
                    .then(commonModifier),
                verticalAlignment = style.dimension.alignment.toAlignment(),
                horizontalArrangement = style.dimension.distribution.toHorizontalArrangement(
                    spacing = style.spacing,
                ),
            ) { content() }

            is Dimension.Vertical -> Column(
                modifier = modifier
                    .size(style.size, horizontalAlignment = style.dimension.alignment.toAlignment())
                    .then(commonModifier),
                verticalArrangement = style.dimension.distribution.toVerticalArrangement(
                    spacing = style.spacing,
                ),
                horizontalAlignment = style.dimension.alignment.toAlignment(),
            ) { content() }

            is Dimension.ZLayer -> Box(
                modifier = modifier
                    .size(
                        size = style.size,
                        horizontalAlignment = style.dimension.alignment.toHorizontalAlignmentOrNull(),
                        verticalAlignment = style.dimension.alignment.toVerticalAlignmentOrNull(),
                    )
                    .then(commonModifier),
                contentAlignment = style.dimension.alignment.toAlignment(),
            ) { content() }
        }
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_Vertical() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                visible = true,
                children = previewChildren(),
                dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyle.Color(Solid(Color.Red)),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = BorderStyle(width = 2.dp, color = Solid(Color.Blue)),
                shadow = ShadowStyle(color = Solid(Color.Black), radius = 10.dp, x = 0.dp, y = 3.dp),
            ),
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_Horizontal() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                visible = true,
                children = previewChildren(),
                dimension = Dimension.Horizontal(alignment = VerticalAlignment.CENTER, distribution = START),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyle.Color(Solid(Color.Red)),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = BorderStyle(width = 2.dp, color = Solid(Color.Blue)),
                shadow = ShadowStyle(color = Solid(Color.Black), radius = 30.dp, x = 0.dp, y = 5.dp),
            ),
        )
    }
}

@Preview
@Composable
private fun StackComponentView_Preview_ZLayer() {
    Box(
        modifier = Modifier.padding(all = 32.dp),
    ) {
        StackComponentView(
            style = StackComponentStyle(
                visible = true,
                children = listOf(
                    TextComponentStyle(
                        visible = true,
                        text = "Hello",
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR,
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Yellow.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                        margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0),
                    ),
                    TextComponentStyle(
                        visible = true,
                        text = "World",
                        color = ColorScheme(
                            light = ColorInfo.Hex(Color.Black.toArgb()),
                        ),
                        fontSize = FontSize.BODY_M,
                        fontWeight = FontWeight.REGULAR,
                        fontFamily = null,
                        textAlign = HorizontalAlignment.CENTER,
                        horizontalAlignment = HorizontalAlignment.CENTER,
                        backgroundColor = ColorScheme(
                            light = ColorInfo.Hex(Color.Blue.toArgb()),
                        ),
                        size = Size(width = Fit, height = Fit),
                        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
                    ),
                ),
                dimension = Dimension.ZLayer(alignment = TwoDimensionalAlignment.BOTTOM_TRAILING),
                size = Size(width = Fit, height = Fit),
                spacing = 16.dp,
                background = BackgroundStyle.Color(Solid(Color.Red)),
                padding = PaddingValues(all = 16.dp),
                margin = PaddingValues(all = 16.dp),
                shape = RoundedCornerShape(size = 20.dp),
                border = BorderStyle(width = 2.dp, color = Solid(Color.Blue)),
                shadow = ShadowStyle(color = Solid(Color.Black), radius = 20.dp, x = 5.dp, y = 5.dp),
            ),
        )
    }
}

@Composable
private fun previewChildren() = listOf(
    TextComponentStyle(
        visible = true,
        text = "Hello",
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR,
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER,
        horizontalAlignment = HorizontalAlignment.CENTER,
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
    ),
    TextComponentStyle(
        visible = true,
        text = "World",
        color = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
        ),
        fontSize = FontSize.BODY_M,
        fontWeight = FontWeight.REGULAR,
        fontFamily = null,
        textAlign = HorizontalAlignment.CENTER,
        horizontalAlignment = HorizontalAlignment.CENTER,
        backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.Blue.toArgb()),
        ),
        size = Size(width = Fit, height = Fit),
        padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
        margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 0.0),
    ),
)
