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

        // Dictionary to count occurrences of each quantized color
        val colorCounts = mutableMapOf<Int, Int>()

        // Calculate step size to sample approximately maxPixelSamples pixels
        val sampleStep = maxOf(1, totalPixels / ColorExtractionConstants.MAX_PIXEL_SAMPLES)

        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        @Suppress("MagicNumber")
        for (pixel in pixels.indices step sampleStep) {
            // Bit shifting and applying the max color component value to each channel
            val color = pixels[pixel]
            val alpha = (color shr 24) and 0xFF
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF

            // Skip pixels that are mostly transparent
            if (alpha < ColorExtractionConstants.MINIMUM_ALPHA_THRESHOLD) continue

            // Quantize colors by reducing precision
            val divisor = ColorExtractionConstants.COLOR_QUANTIZATION_DIVISOR
            val quantizedR = (red / divisor) * divisor
            val quantizedG = (green / divisor) * divisor
            val quantizedB = (blue / divisor) * divisor

            // Calculate simple brightness as sum of RGB components
            val brightness = quantizedR + quantizedG + quantizedB

            // Skip very dark and very bright colors
            if (brightness < ColorExtractionConstants.MINIMUM_BRIGHTNESS_THRESHOLD ||
                brightness > ColorExtractionConstants.MAXIMUM_BRIGHTNESS_THRESHOLD
            ) {
                continue
            }

            // Pack RGB into a single Int for use as map key
            val key = (quantizedR shl 16) or (quantizedG shl 8) or quantizedB
            colorCounts[key] = colorCounts.getOrDefault(key, 0) + 1
        }

        // Sort colors by frequency (most common first)
        val sortedColors = colorCounts.entries.sortedByDescending { it.value }

        val prominentColors = mutableListOf<Color>()
        val black = Triple(0.0, 0.0, 0.0)
        val white = Triple(1.0, 1.0, 1.0)

        for ((colorKey, _) in sortedColors) {
            // bit shifting and converting to double
            val red = ((colorKey shr 16) and 0xFF) / 255.0
            val green = ((colorKey shr 8) and 0xFF) / 255.0
            val blue = (colorKey and 0xFF) / 255.0

            val colorTuple = Triple(red, green, blue)

            // Skip colors too close to black or white
            if (colorDistance(colorTuple, black) < ColorExtractionConstants.MINIMUM_DISTANCE_FROM_BLACK_WHITE ||
                colorDistance(colorTuple, white) < ColorExtractionConstants.MINIMUM_DISTANCE_FROM_BLACK_WHITE
            ) {
                continue
            }

            val newColor = Color(red.toFloat(), green.toFloat(), blue.toFloat())

            // Check if this color is too similar to any already-selected color
            val isTooSimilar = prominentColors.any { existingColor ->
                colorDistance(colorTuple, existingColor.toTriple()) < ColorExtractionConstants.MINIMUM_COLOR_DISTANCE
            }

            if (!isTooSimilar) {
                prominentColors.add(newColor)
                if (prominentColors.size >= count) break
            }
        }

        return prominentColors
    }

    private fun Color.toTriple(): Triple<Double, Double, Double> {
        return Triple(red.toDouble(), green.toDouble(), blue.toDouble())
    }
}
