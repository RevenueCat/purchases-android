package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ui.revenuecatui.helpers.AppStyleExtractor

/**
 * Utility for generating deterministic images used in previews and tests.
 */
internal object DualColorImageGenerator {
    private const val PURPLE_COLOR_HEX = 0xFF800080
    private const val ORANGE_COLOR_HEX = 0xFFFFA500

    internal data class PreviewAppIcon(
        val bitmap: Bitmap,
        val prominentColors: List<Color>,
    )

    val redGreen: PreviewAppIcon by lazy {
        create(color1 = Color.Red, color2 = Color.Green) ?: error("Failed to generate redGreen preview icon")
    }
    val blueGreen: PreviewAppIcon by lazy {
        create(color1 = Color.Blue, color2 = Color.Green) ?: error("Failed to generate blueGreen preview icon")
    }

    val purpleOrange: PreviewAppIcon by lazy {
        create(color1 = Color(PURPLE_COLOR_HEX), color2 = Color(ORANGE_COLOR_HEX))
            ?: error("Failed to generate purpleOrange preview icon")
    }

    fun create(
        color1: Color,
        color2: Color,
        width: Int = DEFAULT_IMAGE_SIZE,
        height: Int = DEFAULT_IMAGE_SIZE,
    ): PreviewAppIcon? {
        val bitmap = createBitmap(
            color1 = color1.toArgb(),
            color2 = color2.toArgb(),
            width = width,
            height = height,
        ) ?: return null
        return PreviewAppIcon(
            bitmap = bitmap,
            prominentColors = AppStyleExtractor.extractProminentColors(
                bitmap = bitmap,
                count = 2,
            ),
        )
    }

    private fun createBitmap(
        color1: Int,
        color2: Int,
        width: Int,
        height: Int,
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val splitWidth = width / 2f

        paint.color = color1
        canvas.drawRect(0f, 0f, splitWidth, height.toFloat(), paint)

        paint.color = color2
        canvas.drawRect(splitWidth, 0f, width.toFloat(), height.toFloat(), paint)

        return bitmap
    }

    private const val DEFAULT_IMAGE_SIZE = 200
}
