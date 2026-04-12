package com.revenuecat.purchases.paywalls

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

private val rgbaColorRegex = Regex("^#([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})?$")
private const val HEX_RADIX = 16
private const val DEFAULT_ALPHA_HEX = "FF"
private const val ALPHA_SHIFT_BITS = 24
private const val RED_SHIFT_BITS = 16
private const val GREEN_SHIFT_BITS = 8

/**
 *
 * Parses the provided [stringRepresentation] into a [color integer][ColorInt].
 *
 * **Note:** the (expected) position of the alpha value is different in the input string compared to the output integer.
 *
 * @param stringRepresentation A color string, which may be any of the following:
 * * RRGGBB**AA**
 * * RRGGBB
 * * One of: `red`, `blue`, `green`, `black`, `white`, `gray`, `cyan`, `magenta`, `yellow`, `lightgray`, `darkgray`,
 * `grey`, `lightgrey`, `darkgrey`, `aqua`, `fuchsia`, `lime`, `maroon`, `navy`, `olive`, `purple`, `silver`, and
 * `teal`.
 *
 * @return A packed [color integer][ColorInt] in **AA**RRGGBB format.
 */
@ColorInt
internal fun parseRGBAColor(stringRepresentation: String): Int =
    rgbaColorRegex.matchEntire(stringRepresentation)?.let { match ->
        val (r, g, b) = match.destructured
        val a = match.groupValues.getOrNull(4).takeUnless { it.isNullOrBlank() } ?: DEFAULT_ALPHA_HEX
        colorInt(
            alpha = a.toInt(HEX_RADIX),
            red = r.toInt(HEX_RADIX),
            green = g.toInt(HEX_RADIX),
            blue = b.toInt(HEX_RADIX),
        )
    } ?: Color.parseColor(stringRepresentation)

/**
 * Copied from Android's Color.argb(), making it slightly more testable as it avoids the need for mocking or
 * Robolectric test runners.
 */
@ColorInt
internal fun colorInt(
    @IntRange(from = 0, to = 255) alpha: Int,
    @IntRange(from = 0, to = 255) red: Int,
    @IntRange(from = 0, to = 255) green: Int,
    @IntRange(from = 0, to = 255) blue: Int,
): Int = (alpha shl ALPHA_SHIFT_BITS) or (red shl RED_SHIFT_BITS) or (green shl GREEN_SHIFT_BITS) or blue
