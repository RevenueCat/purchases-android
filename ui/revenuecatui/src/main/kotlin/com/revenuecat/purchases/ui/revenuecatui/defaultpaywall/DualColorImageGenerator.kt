package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.ui.revenuecatui.helpers.AppStyleExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Utility for generating deterministic images used in previews and tests.
 */
internal object DualColorImageGenerator {

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
        create(color1 = Color(0xFF800080), color2 = Color(0xFFFFA500)) ?:
            error("Failed to generate purpleOrange preview icon")
    }
    val blackWhite: PreviewAppIcon by lazy {
        create(color1 = Color.Black, color2 = Color.White) ?: error("Failed to generate blackWhite preview icon")
    }

    fun singleColor(
        color: Color,
        width: Int = DEFAULT_IMAGE_SIZE,
        height: Int = DEFAULT_IMAGE_SIZE,
    ): PreviewAppIcon? {
        return create(color1 = color, color2 = color, width = width, height = height)
    }

    fun transparent(
        width: Int = DEFAULT_IMAGE_SIZE,
        height: Int = DEFAULT_IMAGE_SIZE,
    ): Bitmap? {
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.TRANSPARENT)
        }
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
            prominentColors = runBlocking {
                AppStyleExtractor.getProminentColorsFromBitmap(
                    bitmap = bitmap,
                    count = 2,
                    dispatcher = Dispatchers.Main,
                )
            },
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
