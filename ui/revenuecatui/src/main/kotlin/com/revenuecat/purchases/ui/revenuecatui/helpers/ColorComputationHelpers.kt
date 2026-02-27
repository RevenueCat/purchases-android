@file:Suppress("MatchingDeclarationName")

package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Constants for color extraction algorithm.
 */
internal object ColorExtractionConstants {
    const val BITS_PER_COMPONENT = 8
    const val BYTES_PER_PIXEL = 4
    const val COLOR_COMPONENT_MASK = 0xFF
    const val RED_CHANNEL_SHIFT = 16
    const val GREEN_CHANNEL_SHIFT = 8
    const val ALPHA_CHANNEL_SHIFT = 24
    const val RGB_NORMALIZATION_DIVISOR = 255.0
    const val MAX_PIXEL_SAMPLES = 10_000
    const val COLOR_QUANTIZATION_DIVISOR = 16
    const val MINIMUM_ALPHA_THRESHOLD = 128
    const val MINIMUM_BRIGHTNESS_THRESHOLD = 50
    const val MAXIMUM_BRIGHTNESS_THRESHOLD = 700
    const val MINIMUM_COLOR_DISTANCE = 0.25
    const val MINIMUM_DISTANCE_FROM_BLACK_WHITE = 0.3
    const val WCAG_COMPONENT_THRESHOLD = 0.03928f
    const val WCAG_LINEAR_COMPONENT_DIVISOR = 12.92
    const val WCAG_GAMMA_OFFSET = 0.055
    const val WCAG_GAMMA_SCALE = 1.055
    const val WCAG_GAMMA_EXPONENT = 2.4
    const val WCAG_RED_LUMINANCE_WEIGHT = 0.2126
    const val WCAG_GREEN_LUMINANCE_WEIGHT = 0.7152
    const val WCAG_BLUE_LUMINANCE_WEIGHT = 0.0722
    const val WCAG_CONTRAST_OFFSET = 0.05
}

/**
 * Calculate Euclidean distance between two colors in RGB space.
 */
internal fun colorDistance(color1: Triple<Double, Double, Double>, color2: Triple<Double, Double, Double>): Double {
    val (r1, g1, b1) = color1
    val (r2, g2, b2) = color2
    return sqrt((r1 - r2).pow(2) + (g1 - g2).pow(2) + (b1 - b2).pow(2))
}

/**
 * Calculate relative luminance per WCAG 2.0.
 * https://www.w3.org/TR/WCAG20/#relativeluminancedef
 */
internal fun relativeLuminance(color: Color): Double {
    fun adjust(component: Float): Double {
        return if (component <= ColorExtractionConstants.WCAG_COMPONENT_THRESHOLD) {
            component / ColorExtractionConstants.WCAG_LINEAR_COMPONENT_DIVISOR
        } else {
            val normalizedComponent = (component + ColorExtractionConstants.WCAG_GAMMA_OFFSET) /
                ColorExtractionConstants.WCAG_GAMMA_SCALE
            normalizedComponent.pow(ColorExtractionConstants.WCAG_GAMMA_EXPONENT)
        }
    }
    return ColorExtractionConstants.WCAG_RED_LUMINANCE_WEIGHT * adjust(color.red) +
        ColorExtractionConstants.WCAG_GREEN_LUMINANCE_WEIGHT * adjust(color.green) +
        ColorExtractionConstants.WCAG_BLUE_LUMINANCE_WEIGHT * adjust(color.blue)
}

/**
 * Calculate contrast ratio between two colors per WCAG 2.0.
 * https://www.w3.org/TR/WCAG20/#contrast-ratiodef
 */
internal fun contrastRatio(color1: Color, color2: Color): Double {
    val l1 = relativeLuminance(color1)
    val l2 = relativeLuminance(color2)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + ColorExtractionConstants.WCAG_CONTRAST_OFFSET) /
        (darker + ColorExtractionConstants.WCAG_CONTRAST_OFFSET)
}

/**
 * Select the color with best contrast against a background color.
 */
internal fun selectColorWithBestContrast(from: List<Color>, againstColor: Color): Color {
    return from.maxByOrNull { contrastRatio(it, againstColor) } ?: againstColor
}
