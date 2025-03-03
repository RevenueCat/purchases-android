@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.START
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.previewTextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Standard luminance coefficients for sRGB (per ITU-R BT.709)
private const val COEFFICIENT_LUMINANCE_RED = 0.299f
private const val COEFFICIENT_LUMINANCE_GREEN = 0.587f
private const val COEFFICIENT_LUMINANCE_BLUE = 0.114f

// The brightness value below which we'll use a white progress indicator
private const val BRIGHTNESS_CUTOFF = 0.6f

@Suppress("LongMethod")
@Composable
internal fun ButtonComponentView(
    style: ButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get a ButtonComponentState that calculates the stateful properties we should use.
    val buttonState = rememberButtonComponentState(
        style = style,
        paywallState = state,
    )

    val coroutineScope = rememberCoroutineScope()
    var isClickable by remember { mutableStateOf(true) }

    val contentAlpha by remember { derivedStateOf { if (isClickable) 1f else 0f } }
    val progressAlpha by remember { derivedStateOf { if (isClickable) 0f else 1f } }
    val animatedContentAlpha by animateFloatAsState(targetValue = contentAlpha)
    val animatedProgressAlpha by animateFloatAsState(targetValue = progressAlpha)

    // We are using a custom Layout instead of a Box to properly handle the case where the StackComponentView is
    // smaller than the CircularProgressIndicator, in either dimension. In this case, we want the
    // CircularProgressIndicator to shrink so it doesn't exceed the StackComponentView's bounds. Using IntrinsicSize
    // and matchParentSize() was considered, but in the end a custom Layout seemed to be the only reliable option.
    Layout(
        content = {
            StackComponentView(
                style = style.stackComponentStyle,
                state = state,
                // We're the button, so we're handling the click already.
                clickHandler = { },
                contentAlpha = animatedContentAlpha,
            )
            CircularProgressIndicator(
                modifier = Modifier.alpha(animatedProgressAlpha),
                color = progressColorFor(style.stackComponentStyle.background),
            )
        },
        modifier = modifier.clickable(enabled = isClickable) {
            isClickable = false
            coroutineScope.launch {
                onClick(buttonState.action)
                isClickable = true
            }
        },
        measurePolicy = { measurables, constraints ->
            val stack = measurables[0].measure(constraints)
            // Ensure that the progress indicator is not bigger than the stack.
            val progress = measurables[1].measure(
                constraints.copy(maxHeight = stack.height, maxWidth = stack.width),
            )
            val totalWidth = stack.width
            val totalHeight = stack.height
            layout(
                width = totalWidth,
                height = totalHeight,
            ) {
                stack.placeRelative(x = 0, y = 0)
                // Center the progress indicator.
                progress.placeRelative(
                    x = ((totalWidth / 2f) - (progress.width / 2f)).roundToInt(),
                    y = ((totalHeight / 2f) - (progress.height / 2f)).roundToInt(),
                )
            }
        },
    )
}

@Composable
private fun progressColorFor(backgroundStyles: BackgroundStyles?): Color {
    if (backgroundStyles == null) return if (isSystemInDarkTheme()) Color.White else Color.Black

    return when (backgroundStyles) {
        is BackgroundStyles.Color -> progressColorFor(backgroundStyles.color.forCurrentTheme)
        is BackgroundStyles.Image -> Color.White
    }
}

private fun progressColorFor(colorStyle: ColorStyle): Color =
    when (colorStyle) {
        is ColorStyle.Solid -> if (colorStyle.color.brightness > BRIGHTNESS_CUTOFF) Color.Black else Color.White
        is ColorStyle.Gradient -> {
            val averageBrightness = colorStyle.brush.colors.map { it.brightness }.average()
            if (averageBrightness > BRIGHTNESS_CUTOFF) Color.Black else Color.White
        }
    }

private val Color.brightness: Float
    get() = red * COEFFICIENT_LUMINANCE_RED +
        green * COEFFICIENT_LUMINANCE_GREEN +
        blue * COEFFICIENT_LUMINANCE_BLUE

@Preview
@Composable
private fun ButtonComponentView_Preview_Default() {
    ButtonComponentView(previewButtonComponentStyle(), previewEmptyState(), { })
}

@Preview
@Composable
private fun ButtonComponentView_Preview_Narrow() {
    ButtonComponentView(
        style = previewButtonComponentStyle(
            stackComponentStyle = previewStackComponentStyle(
                children = listOf(
                    previewTextComponentStyle(
                        text = "Restore purchases",
                        backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Yellow)),
                    ),
                ),
            ),
        ),
        state = previewEmptyState(),
        onClick = { },
    )
}

@Composable
private fun previewButtonComponentStyle(
    stackComponentStyle: StackComponentStyle = StackComponentStyle(
        children = listOf(
            previewTextComponentStyle(
                text = "Restore purchases",
                backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Yellow)),
                size = Size(width = Fit, height = Fit),
                padding = Padding(top = 8.0, bottom = 8.0, leading = 8.0, trailing = 8.0),
                margin = Padding(top = 0.0, bottom = 24.0, leading = 0.0, trailing = 24.0),
            ),
        ),
        dimension = Dimension.Vertical(alignment = HorizontalAlignment.CENTER, distribution = START),
        visible = true,
        size = Size(width = Fit, height = Fit),
        spacing = 16.dp,
        background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.Red))),
        padding = PaddingValues(all = 16.dp),
        margin = PaddingValues(all = 16.dp),
        shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
        border = BorderStyles(width = 2.dp, colors = ColorStyles(light = ColorStyle.Solid(Color.Blue))),
        shadow = ShadowStyles(
            colors = ColorStyles(ColorStyle.Solid(Color.Black)),
            radius = 10.dp,
            x = 0.dp,
            y = 3.dp,
        ),
        badge = null,
        scrollOrientation = null,
        rcPackage = null,
        tabIndex = null,
        overrides = emptyList(),
    ),
    action: ButtonComponentStyle.Action = ButtonComponentStyle.Action.RestorePurchases,
): ButtonComponentStyle {
    return ButtonComponentStyle(
        stackComponentStyle = stackComponentStyle,
        action = action,
    )
}
