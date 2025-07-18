@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle

@JvmSynthetic
@Stable
internal fun Modifier.border(
    border: BorderStyle,
    shape: Shape = RectangleShape,
): Modifier = border(width = border.width, colorStyle = border.color, shape = shape)

@JvmSynthetic
@Stable
internal fun Modifier.border(width: Dp, colorStyle: ColorStyle, shape: Shape): Modifier =
    when (colorStyle) {
        is ColorStyle.Solid -> this.border(width = width, color = colorStyle.color, shape = shape)
        is ColorStyle.Gradient -> this.border(width = width, brush = colorStyle.brush, shape = shape)
    }

@Suppress("MagicNumber")
@Preview("Solid")
@Composable
private fun Border_Preview_Solid() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(Color.Red)
            .border(
                border = BorderStyle(
                    width = 10.dp,
                    color = ColorStyle.Solid(color = Color.Blue),
                ),
            ),
    )
}

@Suppress("MagicNumber")
@Preview("SolidThin")
@Composable
private fun Border_Preview_SolidThin() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(Color.Red)
            .border(
                border = BorderStyle(
                    width = 2.dp,
                    color = ColorStyle.Solid(color = Color.Blue),
                ),
            ),
    )
}

@Suppress("MagicNumber")
@Preview("SolidCircle")
@Composable
private fun Border_Preview_SolidCircle() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(Color.Red)
            .border(
                border = BorderStyle(
                    width = 10.dp,
                    color = ColorStyle.Solid(color = Color.Blue),
                ),
                shape = CircleShape,
            ),
    )
}

@Preview("LinearGradientSquare")
@Composable
private fun Border_Preview_LinearGradientSquare() {
    Border_Preview_LinearGradient(shape = RectangleShape)
}

@Preview("LinearGradientCircle")
@Composable
private fun Border_Preview_LinearGradientCircle() {
    Border_Preview_LinearGradient(shape = CircleShape)
}

@Preview("RadialGradientSquare")
@Composable
private fun Border_Preview_RadialGradientSquare() {
    Border_Preview_RadialGradient(shape = RectangleShape)
}

@Preview("RadialGradientCircle")
@Composable
private fun Border_Preview_RadialGradientCircle() {
    Border_Preview_RadialGradient(shape = CircleShape)
}

@Suppress("MagicNumber")
@Composable
private fun Border_Preview_LinearGradient(shape: Shape) {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(Color.Red)
            .border(
                border = BorderStyle(
                    width = 10.dp,
                    color = ColorInfo.Gradient.Linear(
                        degrees = 135f,
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.Cyan.toArgb(),
                                percent = 10f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                                percent = 30f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                                percent = 80f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
                shape = shape,
            ),
    )
}

@Suppress("MagicNumber")
@Composable
private fun Border_Preview_RadialGradient(shape: Shape) {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(Color.Red)
            .border(
                border = BorderStyle(
                    width = 10.dp,
                    color = ColorInfo.Gradient.Radial(
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.Cyan.toArgb(),
                                percent = 80f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color(red = 0x00, green = 0x66, blue = 0xff).toArgb(),
                                percent = 90f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color(red = 0xA0, green = 0x00, blue = 0xA0).toArgb(),
                                percent = 96f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
                shape = shape,
            ),
    )
}
