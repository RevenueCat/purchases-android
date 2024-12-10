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
 * @param width The number of pixels to read from each row.
 * @param height The number of rows to read.
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
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = width,
        height = height,
    )

    return assert(
        SemanticsMatcher("All pixels have color '$color'") {
            pixels.all { pixel ->
                if (pixel == colorArgbInt) true
                else {
                    println("Found a pixel with a different color: ${Color(pixel)}")
                    false
                }
            }
        }
    )
}

/**
 * Assert the number of pixels in a rectangular area of this Composable that have the provided [color].
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
 * @param width The number of pixels to read from each row.
 * @param height The number of rows to read.
 * @param color The color to assert.
 * @param predicate The assertion you want to run.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertPixelColorCount(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    color: Color,
    predicate: (count: Int) -> Boolean,
): SemanticsNodeInteraction {
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = width,
        height = height,
    )

    return assert(
        SemanticsMatcher("Assert count of pixels with color '$color'") {
            val count = pixels
                .groupBy { color -> color }
                .mapValues { (_, pixels) -> pixels.count() }
                .getOrDefault(color.toArgb(), defaultValue = 0)

            predicate(count)
        }
    )
}

/**
 * Assert the percentage of pixels in a rectangular area of this Composable that have the provided [color].
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
 * @param width The number of pixels to read from each row.
 * @param height The number of rows to read.
 * @param color The color to assert.
 * @param predicate The assertion you want to run. The `percentage` parameter is in range 0..100.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertPixelColorPercentage(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    color: Color,
    predicate: (percentage: Float) -> Boolean,
): SemanticsNodeInteraction =
    assertPixelColorCount(
        startX = startX,
        startY = startY,
        width = width,
        height = height,
        color = color,
        predicate = { count ->
            val percentage = count.toFloat() / (width * height)
            predicate(percentage)
        }
    )


/**
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 */
private fun SemanticsNodeInteraction.readPixels(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
): IntArray {
    val pixels = IntArray(width * height)

    captureToImageCompat().readPixels(
        buffer = pixels,
        startX = startX,
        startY = startY,
        width = width,
        height = height,
    )

    return pixels
}
