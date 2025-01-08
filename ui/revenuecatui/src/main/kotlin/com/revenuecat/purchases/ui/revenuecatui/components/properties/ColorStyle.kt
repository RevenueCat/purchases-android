package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.colorsForCurrentTheme
import dev.drewhamilton.poko.Poko
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Used to normalize [ColorInfo.Gradient.Point.percent] values (which are in the range 0..100) to a range of 0..1.
 */
private const val PERCENT_SCALE = 100f

/**
 * Ready to use color properties for the current theme.
 */
internal sealed interface ColorStyle {

    @JvmInline
    value class Solid(@JvmSynthetic val color: Color) : ColorStyle

    @JvmInline
    value class Gradient(@JvmSynthetic val brush: Brush) : ColorStyle
}

@JvmSynthetic
@Composable
internal fun rememberColorStyle(scheme: ColorScheme): ColorStyle {
    val colorInfo = scheme.colorsForCurrentTheme
    return remember(colorInfo) { colorInfo.toColorStyle() }
}

@JvmSynthetic
@Composable
internal fun ColorScheme.toColorStyle(): ColorStyle = colorsForCurrentTheme.toColorStyle()

@JvmSynthetic
internal fun ColorInfo.toColorStyle(): ColorStyle {
    return when (this) {
        is ColorInfo.Alias -> TODO("Color aliases are not yet implemented.")
        is ColorInfo.Hex -> ColorStyle.Solid(Color(color = value))
        is ColorInfo.Gradient -> ColorStyle.Gradient(
            when (this) {
                is ColorInfo.Gradient.Linear -> relativeLinearGradient(
                    colorStops = points.toColorStops(),
                    degrees = degrees,
                )

                is ColorInfo.Gradient.Radial -> Brush.radialGradient(
                    colorStops = points.toColorStops(),
                )
            },
        )
    }
}

private fun List<ColorInfo.Gradient.Point>.toColorStops(): Array<Pair<Float, Color>> =
    map { point -> point.percent / PERCENT_SCALE to Color(point.color) }
        .toTypedArray()

@Stable
private fun relativeLinearGradient(
    vararg colorStops: Pair<Float, Color>,
    degrees: Float,
    tileMode: TileMode = TileMode.Clamp,
): ShaderBrush {
    val end = RelativeEndOffset(degrees)
    return relativeLinearGradient(
        colorStops = colorStops,
        start = Offset(x = 1f, y = 1f) - end,
        end = end,
        tileMode = tileMode,
    )
}

@Stable
private fun relativeLinearGradient(
    vararg colorStops: Pair<Float, Color>,
    start: Offset,
    end: Offset,
    tileMode: TileMode = TileMode.Clamp,
): ShaderBrush =
    RelativeLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        start = start,
        end = end,
        tileMode = tileMode,
    )

@Suppress("FunctionName")
private fun RelativeEndOffset(degrees: Float): Offset {
    // Convert the angle to radians.
    val radians = Math.toRadians(degrees.toDouble())

    // Calculate the normalized x and y positions.
    val xPosition = cos(radians)
    val yPosition = sin(radians)

    // Determine the scaling factor to move the point to the edge of the enclosing square.
    val scaleFactor = max(abs(xPosition), abs(yPosition))

    // Scale the x and y coordinates.
    val scaledX = xPosition / scaleFactor
    val scaledY = yPosition / scaleFactor

    // Convert the scaled coordinates to an Offset.
    val x = (scaledX + 1) / 2f
    val y = (1 - scaledY) / 2f

    return Offset(x.toFloat(), y.toFloat())
}

/**
 * A brush that uses relative coordinates to draw the linear gradient, instead of absolute ones. Coordinates passed to
 * [start] and [end] must be between 0 and 1 (inclusive).
 *
 * @param start Relative start coordinates. `Offset(x = 0f, y = 0f)` indicates the top left of the drawing area.
 * @param end Relative end coordinates. `Offset(x = 1f, y = 1f)` indicates the bottom right of the drawing area.
 */
@Poko
@Immutable
private class RelativeLinearGradient(
    private val colors: List<Color>,
    private val stops: List<Float>? = null,
    private val start: Offset,
    private val end: Offset,
    private val tileMode: TileMode = TileMode.Clamp,
) : ShaderBrush() {

    init {
        require(start.x in 0f..1f) {
            "Coordinates must be between 0 and 1 (inclusive). `start.x` is ${start.x}"
        }
        require(start.y in 0f..1f) {
            "Coordinates must be between 0 and 1 (inclusive). `start.y` is ${start.y}"
        }
        require(end.x in 0f..1f) {
            "Coordinates must be between 0 and 1 (inclusive). `end.x` is ${end.x}"
        }
        require(end.y in 0f..1f) {
            "Coordinates must be between 0 and 1 (inclusive). `end.y` is ${end.y}"
        }
    }

    override fun createShader(size: Size): Shader {
        val startPx = Offset(start.x * size.width, start.y * size.height)
        val endPx = Offset(end.x * size.width, end.y * size.height)
        return LinearGradientShader(
            colors = colors,
            colorStops = stops,
            from = startPx,
            to = endPx,
            tileMode = tileMode,
        )
    }
}

@Suppress("MagicNumber")
@Preview("Square")
@Composable
private fun LinearGradient_Preview_Square() {
    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color.Yellow,
                        0.5f to Color.Red,
                        1f to Color.Blue,
                    ),
                    degrees = 45f,
                ),
            ),
    )
}

@Suppress("MagicNumber")
@Preview("Rectangle")
@Composable
private fun LinearGradient_Preview_Rectangle() {
    Box(
        modifier = Modifier
            .requiredSize(300.dp, 100.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color.Yellow,
                        0.5f to Color.Red,
                        1f to Color.Blue,
                    ),
                    degrees = 45f,
                ),
            ),
    )
}
