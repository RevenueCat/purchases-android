@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate
import dev.drewhamilton.poko.Poko
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
    value class Gradient(@JvmSynthetic val brush: GradientBrush) : ColorStyle
}

/**
 * [ColorStyle]s for light and dark mode.
 */
internal data class ColorStyles(
    @get:JvmSynthetic val light: ColorStyle,
    @get:JvmSynthetic val dark: ColorStyle? = null,
)

internal val ColorStyles.forCurrentTheme: ColorStyle
    @JvmSynthetic @Composable
    get() = if (isSystemInDarkTheme()) dark ?: light else light

/**
 * This is just a convenience method that can be used in previews or tests, as it is easier to specify a gradient in
 * color stops in a [ColorInfo.Gradient] than it is to create a brush to be used in [ColorStyle.Gradient].
 */
@JvmSynthetic
internal fun ColorInfo.Gradient.toColorStyle(): ColorStyle =
    ColorStyle.Gradient(
        when (this) {
            is ColorInfo.Gradient.Linear -> relativeLinearGradient(
                colorStops = points.toColorStops(),
                degrees = degrees,
            )

            is ColorInfo.Gradient.Radial -> radialGradient(
                colorStops = points.toColorStops(),
            )
        },
    )

@JvmSynthetic
internal fun ColorScheme.toColorStyles(
    aliases: Map<ColorAlias, ColorScheme>,
): Result<ColorStyles, NonEmptyList<PaywallValidationError>> =
    zipOrAccumulate(
        first = light.toColorStyle(aliases, useLightAlias = true)
            .mapError { nonEmptyListOf(it) },
        second = dark?.toColorStyle(aliases, useLightAlias = false)
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
    ) { light, dark ->
        ColorStyles(light = light, dark = dark)
    }

/**
 * Converts a [ColorInfo] to a [ColorStyle], using [aliases] for lookup if this is a [ColorInfo.Alias].
 *
 * @param useLightAlias Since [aliases] maps to entire [ColorScheme]s, we need to know which [ColorInfo] to get from
 * that scheme. If this is true, we'll use the light one. If this is false, we'll use the dark one if available, and
 * light otherwise.
 */
@JvmSynthetic
internal fun ColorInfo.toColorStyle(
    aliases: Map<ColorAlias, ColorScheme>,
    useLightAlias: Boolean,
): Result<ColorStyle, PaywallValidationError> {
    return when (this) {
        is ColorInfo.Alias -> {
            val aliasedScheme = aliases[value]
            val aliasedInfo = aliasedScheme?.let {
                if (useLightAlias) aliasedScheme.light else aliasedScheme.dark ?: aliasedScheme.light
            }
            when (aliasedInfo) {
                is ColorInfo.Gradient,
                is ColorInfo.Hex,
                -> aliasedInfo.toColorStyle(aliases, useLightAlias)
                is ColorInfo.Alias -> Result.Error(PaywallValidationError.AliasedColorIsAlias(value, aliasedInfo.value))
                null -> Result.Error(PaywallValidationError.MissingColorAlias(value))
            }
        }
        is ColorInfo.Hex -> Result.Success(ColorStyle.Solid(Color(color = value)))
        is ColorInfo.Gradient -> Result.Success(
            ColorStyle.Gradient(
                when (this) {
                    is ColorInfo.Gradient.Linear -> relativeLinearGradient(
                        colorStops = points.toColorStops(),
                        degrees = degrees,
                    )

                    is ColorInfo.Gradient.Radial -> radialGradient(
                        colorStops = points.toColorStops(),
                    )
                },
            ),
        )
    }
}

private fun List<ColorInfo.Gradient.Point>.toColorStops(): Array<Pair<Float, Color>> =
    map { point -> point.percent / PERCENT_SCALE to Color(point.color) }
        .toTypedArray()

/**
 * An intermediate type for linear and radial gradients that allows for reading the [colors] after the brush has been
 * constructed.
 */
internal abstract class GradientBrush : ShaderBrush() {
    @get:JvmSynthetic
    internal abstract val colors: List<Color>
}

@Stable
private fun relativeLinearGradient(
    vararg colorStops: Pair<Float, Color>,
    degrees: Float,
    tileMode: TileMode = TileMode.Clamp,
): GradientBrush {
    return RelativeLinearGradient(
        colors = List(colorStops.size) { i -> colorStops[i].second },
        stops = List(colorStops.size) { i -> colorStops[i].first },
        degrees = degrees,
        tileMode = tileMode,
    )
}

/**
 * A brush that uses relative coordinates to draw the linear gradient, instead of absolute ones. It also follows CSS'
 * [linear-gradient](https://developer.mozilla.org/en-US/docs/Web/CSS/gradient/linear-gradient) spec.
 *
 * Heavily inspired by
 * [this blog post by Mukhtar Bimurat](https://medium.com/@bimurat.mukhtar/how-to-implement-linear-gradient-with-any-angle-in-jetpack-compose-3ded798c81f5).
 */
@Suppress("MagicNumber", "ComplexCondition", "MaxLineLength")
@Poko
@Immutable
private class RelativeLinearGradient(
    override val colors: List<Color>,
    private val stops: List<Float>? = null,
    degrees: Float,
    private val tileMode: TileMode = TileMode.Clamp,
) : GradientBrush() {

    // Handling extreme degrees.
    private val degrees: Float = ((90 - degrees) % 360 + 360) % 360
    private val radians: Float = Math.toRadians(this.degrees.toDouble()).toFloat()

    override fun createShader(size: Size): Shader {
        val diagonal = sqrt(size.width.pow(2) + size.height.pow(2))
        val angleBetweenDiagonalAndWidth = acos(size.width / diagonal)
        val angleBetweenDiagonalAndGradientLine =
            if ((degrees > 90 && degrees < 180) || (degrees > 270 && degrees < 360)) {
                PI.toFloat() - radians - angleBetweenDiagonalAndWidth
            } else {
                radians - angleBetweenDiagonalAndWidth
            }
        val halfGradientLine = abs(cos(angleBetweenDiagonalAndGradientLine) * diagonal) / 2

        val horizontalOffset = halfGradientLine * cos(radians)
        val verticalOffset = halfGradientLine * sin(radians)

        val startPx = size.center + Offset(-horizontalOffset, verticalOffset)
        val endPx = size.center + Offset(horizontalOffset, -verticalOffset)

        return LinearGradientShader(
            colors = colors,
            colorStops = stops,
            from = startPx,
            to = endPx,
            tileMode = tileMode,
        )
    }
}

@Stable
private fun radialGradient(
    vararg colorStops: Pair<Float, Color>,
    center: Offset = Offset.Unspecified,
    radius: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): GradientBrush =
    RadialGradient(
        colorStops = colorStops,
        center = center,
        radius = radius,
        tileMode = tileMode,
    )

/**
 * A simple wrapper around [Brush.radialGradient] to make it conform to [GradientBrush].
 */
private class RadialGradient(
    vararg colorStops: Pair<Float, Color>,
    private val center: Offset = Offset.Unspecified,
    private val radius: Float = Float.POSITIVE_INFINITY,
    private val tileMode: TileMode = TileMode.Clamp,
) : GradientBrush() {
    private val colorStopsArray = colorStops

    override val colors: List<Color> = colorStops.map { it.second }

    override fun createShader(size: Size): Shader {
        // Use the larger dimension to ensure the gradient covers the entire shape
        val effectiveRadius = if (radius == Float.POSITIVE_INFINITY) {
            val maxDimension = maxOf(size.width, size.height)
            // Using 0.5 * maxDimension to calculate bigger dimension radius
            maxDimension / 2f
        } else {
            radius
        }

        val effectiveCenter = if (center == Offset.Unspecified) {
            size.center
        } else {
            center
        }

        return RadialGradientShader(
            colors = colorStopsArray.map { it.second },
            colorStops = colorStopsArray.map { it.first },
            center = effectiveCenter,
            radius = effectiveRadius,
            tileMode = tileMode,
        )
    }

    override fun equals(other: Any?): Boolean =
        other is RadialGradient &&
            other.colorStopsArray.contentEquals(colorStopsArray) &&
            other.center == center &&
            other.radius == radius &&
            other.tileMode == tileMode

    override fun hashCode(): Int =
        colorStopsArray.contentHashCode() * 31 +
            center.hashCode() * 31 +
            radius.hashCode() * 31 +
            tileMode.hashCode()

    override fun toString(): String = "RadialGradient(colorStops=${colorStopsArray.contentToString()})"
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
@Preview
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

/**
 * [reference](https://developer.mozilla.org/en-US/docs/Web/CSS/gradient/linear-gradient#gradient_at_a_45-degree_angle)
 */
@Preview
@Composable
private fun LinearGradient_Preview_Rectangle_RedBlue() {
    Box(
        modifier = Modifier
            .requiredSize(300.dp, 55.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color.Red,
                        1f to Color.Blue,
                    ),
                    degrees = 45f,
                ),
            ),
    )
}

/**
 * [reference](https://developer.mozilla.org/en-US/docs/Web/CSS/gradient/linear-gradient#gradient_that_starts_at_60_of_the_gradient_line)
 */
@Suppress("MagicNumber")
@Preview
@Composable
private fun LinearGradient_Preview_Rectangle_OrangeCyan() {
    Box(
        modifier = Modifier
            .requiredSize(300.dp, 55.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0.6f to Color(red = 0xFF, green = 0xA5, blue = 0x00),
                        1f to Color.Cyan,
                    ),
                    degrees = 135f,
                ),
            ),
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun LinearGradient_Preview_Rectangle_Template014Button() {
    Box(
        modifier = Modifier
            .requiredSize(300.dp, 55.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color(red = 0x01, green = 0x01, blue = 0x57),
                        0.46f to Color(red = 0x23, green = 0x23, blue = 0x97),
                        1f to Color(red = 0xDD, green = 0x02, blue = 0x5C),
                    ),
                    degrees = 8f,
                ),
                shape = RoundedCornerShape(50),
            ),
    )
}

/**
 * [reference](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_images/Using_CSS_gradients#using_angles)
 */
@Preview
@Composable
private fun LinearGradient_Preview_Square_BluePink() {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color.Blue,
                        1f to Color(red = 0xFF, green = 0xC0, blue = 0xCB),
                    ),
                    degrees = 70f,
                ),
            ),
    )
}

@Suppress("MagicNumber")
private class DegreesProvider : PreviewParameterProvider<Float> {
    override val values: Sequence<Float> = sequenceOf(
        0f,
        90f,
        180f,
        -90f,
    )
}

/**
 * [reference](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_images/Using_CSS_gradients#using_angles)
 */
@Preview
@Composable
private fun LinearGradient_Preview_SquaresDegrees(
    @PreviewParameter(DegreesProvider::class) degrees: Float,
) {
    Box(
        modifier = Modifier
            .requiredSize(100.dp)
            .background(
                relativeLinearGradient(
                    colorStops = arrayOf(
                        0f to Color.Red,
                        1f to Color.White,
                    ),
                    degrees = degrees,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${degrees}deg",
        )
    }
}

@Preview
@Composable
private fun RadialGradient_Preview() {
    Box(
        modifier = Modifier
            .requiredSize(width = 300.dp, height = 100.dp)
            .background(
                radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Red,
                        1f to Color.White,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
    }
}
