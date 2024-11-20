package com.revenuecat.purchases.paywalls

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

private val rgbaColorRegex = Regex("^#([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})([A-Fa-f0-9]{2})?$")

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
@Suppress("MagicNumber")
@ColorInt
internal fun parseRGBAColor(stringRepresentation: String): Int =
    rgbaColorRegex.matchEntire(stringRepresentation)?.let { match ->
        val radix = 16
        val (r, g, b) = match.destructured
        val a = match.groupValues.getOrNull(4).takeUnless { it.isNullOrBlank() } ?: "FF"
        colorInt(
            alpha = a.toInt(radix),
            red = r.toInt(radix),
            green = g.toInt(radix),
            blue = b.toInt(radix),
        )
    } ?: Color.parseColor(stringRepresentation)

/**
 * Copied from Android's Color.argb(), making it slightly more testable as it avoids the need for mocking or
 * Robolectric test runners.
 */
@Suppress("MagicNumber")
@ColorInt
internal fun colorInt(
    @IntRange(from = 0, to = 255) alpha: Int,
    @IntRange(from = 0, to = 255) red: Int,
    @IntRange(from = 0, to = 255) green: Int,
    @IntRange(from = 0, to = 255) blue: Int,
): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
