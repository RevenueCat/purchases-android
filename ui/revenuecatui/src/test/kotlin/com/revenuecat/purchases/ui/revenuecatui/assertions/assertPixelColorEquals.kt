package com.revenuecat.purchases.ui.revenuecatui.assertions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat

/**
 * Assert that the pixels in a rectangular area of this Composable have the provided [color].
 *
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 *
 * @param startX The x-coordinate of the first pixel to read from the Composable.
 * @param startY The y-coordinate of the first pixel to read from the Composable.
 * @param width The number of pixels to read from each row
 * @param height The number of rows to read
 * @param color The color to assert.
 */
internal fun SemanticsNodeInteraction.assertPixelColorEquals(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    color: Color,
): SemanticsNodeInteraction {
    val colorArgbInt = color.toArgb()
    val pixels = IntArray(width * height)

    captureToImageCompat().readPixels(
        buffer = pixels,
        startX = startX,
        startY = startY,
        width = 4,
        height = 4,
    )

    return assert(
        SemanticsMatcher("Background has color '$color'") {
            pixels.all { pixel -> pixel == colorArgbInt }
        }
    )
}
