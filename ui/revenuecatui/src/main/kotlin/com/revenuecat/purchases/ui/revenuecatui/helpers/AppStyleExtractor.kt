package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts app metadata and visual style information.
 */
internal object AppStyleExtractor {

    /**
     * Get the application display name.
     */
    fun getAppName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val packageManager = context.packageManager
        return applicationInfo.loadLabel(packageManager).toString()
    }

    /**
     * Get the app icon as a Bitmap.
     */
    fun getAppIconBitmap(context: Context): Bitmap? {
        return try {
            val packageManager = context.packageManager
            val drawable = context.applicationInfo.loadIcon(packageManager)
            drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            null
        }
    }

    /**
     * Extract prominent colors from the app icon asynchronously.
     *
     * Algorithm:
     * 1. Samples pixels from the app icon (up to 10,000 samples for performance)
     * 2. Quantizes colors to group similar shades together
     * 3. Filters out transparent, very dark, and very bright pixels
     * 4. Sorts colors by frequency (most common first)
     * 5. Removes colors that are too similar to already-selected colors
     *
     * @param bitmap The bitmap to extract colors from (defaults to app icon)
     * @param count Maximum number of colors to return
     * @return List of prominent colors
     */
    suspend fun getProminentColorsFromBitmap(
        bitmap: Bitmap?,
        count: Int = 2,
    ): List<Color> = withContext(Dispatchers.Default) {
        extractProminentColors(bitmap, count)
    }

    /**
     * Synchronous color extraction for testing/previews.
     */
    internal fun extractProminentColorsSync(bitmap: Bitmap?, count: Int = 2): List<Color> {
        return extractProminentColors(bitmap, count)
    }

    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod", "ReturnCount")
    private fun extractProminentColors(bitmap: Bitmap?, count: Int): List<Color> {
        if (bitmap == null) return emptyList()

        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        if (totalPixels == 0) return emptyList()

        val colorCounts = mutableMapOf<Int, Int>()
        val sampleStep = maxOf(1, totalPixels / ColorExtractionConstants.MAX_PIXEL_SAMPLES)
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixelIndex in pixels.indices step sampleStep) {
            val key = quantizedColorKeyOrNull(pixels[pixelIndex])
            if (key != null) {
                colorCounts[key] = colorCounts.getOrDefault(key, 0) + 1
            }
        }

        val prominentColors = mutableListOf<Color>()
        val black = Triple(0.0, 0.0, 0.0)
        val white = Triple(1.0, 1.0, 1.0)

        for ((colorKey, _) in colorCounts.entries.sortedByDescending { it.value }) {
            val colorTuple = colorKeyToTuple(colorKey)
            val isFarFromBlackAndWhite =
                colorDistance(colorTuple, black) >= ColorExtractionConstants.MINIMUM_DISTANCE_FROM_BLACK_WHITE &&
                    colorDistance(colorTuple, white) >= ColorExtractionConstants.MINIMUM_DISTANCE_FROM_BLACK_WHITE

            val isTooSimilar = prominentColors.any { existingColor ->
                colorDistance(colorTuple, existingColor.toTriple()) < ColorExtractionConstants.MINIMUM_COLOR_DISTANCE
            }

            if (isFarFromBlackAndWhite && !isTooSimilar) {
                prominentColors.add(colorTuple.toColor())
                if (prominentColors.size >= count) break
            }
        }

        return prominentColors
    }

    private fun quantizedColorKeyOrNull(color: Int): Int? {
        val alpha = (color shr ColorExtractionConstants.ALPHA_CHANNEL_SHIFT) and
            ColorExtractionConstants.COLOR_COMPONENT_MASK
        var quantizedKey: Int? = null

        if (alpha >= ColorExtractionConstants.MINIMUM_ALPHA_THRESHOLD) {
            val red = (color shr ColorExtractionConstants.RED_CHANNEL_SHIFT) and
                ColorExtractionConstants.COLOR_COMPONENT_MASK
            val green = (color shr ColorExtractionConstants.GREEN_CHANNEL_SHIFT) and
                ColorExtractionConstants.COLOR_COMPONENT_MASK
            val blue = color and ColorExtractionConstants.COLOR_COMPONENT_MASK

            val divisor = ColorExtractionConstants.COLOR_QUANTIZATION_DIVISOR
            val quantizedR = (red / divisor) * divisor
            val quantizedG = (green / divisor) * divisor
            val quantizedB = (blue / divisor) * divisor
            val brightness = quantizedR + quantizedG + quantizedB
            val isWithinBrightnessRange =
                brightness >= ColorExtractionConstants.MINIMUM_BRIGHTNESS_THRESHOLD &&
                    brightness <= ColorExtractionConstants.MAXIMUM_BRIGHTNESS_THRESHOLD

            if (isWithinBrightnessRange) {
                quantizedKey = (quantizedR shl ColorExtractionConstants.RED_CHANNEL_SHIFT) or
                    (quantizedG shl ColorExtractionConstants.GREEN_CHANNEL_SHIFT) or quantizedB
            }
        }

        return quantizedKey
    }

    private fun colorKeyToTuple(colorKey: Int): Triple<Double, Double, Double> {
        val red = normalizedColorComponent(colorKey, ColorExtractionConstants.RED_CHANNEL_SHIFT)
        val green = normalizedColorComponent(colorKey, ColorExtractionConstants.GREEN_CHANNEL_SHIFT)
        val blue = (colorKey and ColorExtractionConstants.COLOR_COMPONENT_MASK) /
            ColorExtractionConstants.RGB_NORMALIZATION_DIVISOR
        return Triple(red, green, blue)
    }

    private fun normalizedColorComponent(colorKey: Int, shift: Int): Double {
        val component = (colorKey shr shift) and ColorExtractionConstants.COLOR_COMPONENT_MASK
        return component / ColorExtractionConstants.RGB_NORMALIZATION_DIVISOR
    }

    private fun Triple<Double, Double, Double>.toColor(): Color {
        return Color(first.toFloat(), second.toFloat(), third.toFloat())
    }

    private fun Color.toTriple(): Triple<Double, Double, Double> {
        return Triple(red.toDouble(), green.toDouble(), blue.toDouble())
    }
}
