package com.revenuecat.purchases.ui.revenuecatui.assertions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.unit.Dp
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
 * @param color The color to assert.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 */
internal fun SemanticsNodeInteraction.assertPixelColorEquals(
    color: Color,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
): SemanticsNodeInteraction {
    val colorArgbInt = color.toArgb()

    val (widthToUse, heightToUse) = getDimensionsIfNull(width = width, height = height)
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = widthToUse,
        height = heightToUse,
    )

    pixels
        .groupBy { color -> color }
        .mapValues { (_, pixels) -> pixels.count() }
        .also {
            it.forEach { (color, count) ->
                println("TESTING got $count pixel(s) of color ${Color(color)}")
            }
        }

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
 * Assert that the pixels in a rectangular area of this Composable do not have the provided [color].
 *
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 *
 * @param color The color to assert.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 */
internal fun SemanticsNodeInteraction.assertNoPixelColorEquals(
    color: Color,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
): SemanticsNodeInteraction =
    assertPixelColorCount(color = color, startX = startX, startY = startY, width = width, height = height) { it == 0}

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
 * @param color The color to assert.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 * @param predicate The assertion you want to run.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertPixelColorCount(
    color: Color,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
    predicate: (count: Int) -> Boolean,
): SemanticsNodeInteraction {
    val (widthToUse, heightToUse) = getDimensionsIfNull(width = width, height = height)
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = widthToUse,
        height = heightToUse,
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
 * @param color The color to assert.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 * @param predicate The assertion you want to run. The `percentage` parameter is in range 0..100.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertPixelColorPercentage(
    color: Color,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
    predicate: (percentage: Float) -> Boolean,
): SemanticsNodeInteraction {
    val (widthToUse, heightToUse) = getDimensionsIfNull(width = width, height = height)
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = widthToUse,
        height = heightToUse,
    )

    return assert(
        SemanticsMatcher("Assert percentage of pixels with color '$color'") {
            val count = pixels
                .groupBy { color -> color }
                .mapValues { (_, pixels) -> pixels.count() }
                .getOrDefault(color.toArgb(), defaultValue = 0)

            val percentage = count.toFloat() / (widthToUse * heightToUse)

            predicate(percentage)
        }
    )
}

/**
 * Asserts the border color of a rectangular element.
 */
internal fun SemanticsNodeInteraction.assertRectangularBorderColor(
    borderWidth: Dp,
    expectedBorderColor: Color,
    expectedBackgroundColor: Color,
): SemanticsNodeInteraction = run {

    val node = fetchSemanticsNode()
    // Compose seems to have a minimum border width of 2 px. See also Dp.Hairline.
    val borderWidthPx = with(node.layoutInfo.density) { borderWidth.roundToPx() }.coerceAtLeast(2)
    val size = node.size

    // Top edge
    assertPixelColorEquals(
        startX = 0,
        startY = 0,
        width = size.width,
        height = borderWidthPx,
        color = expectedBorderColor
    )
        // Left edge
        .assertPixelColorEquals(
            startX = 0,
            startY = 0,
            width = borderWidthPx,
            height = size.height,
            color = expectedBorderColor
        )
        // Right edge
        .assertPixelColorEquals(
            startX = size.width - borderWidthPx,
            startY = 0,
            width = borderWidthPx,
            height = size.height,
            color = expectedBorderColor
        )
        // Bottom edge
        .assertPixelColorEquals(
            startX = 0,
            startY = size.height - borderWidthPx,
            width = size.width,
            height = borderWidthPx,
            color = expectedBorderColor
        )
        // Inner area
        .assertPixelColorEquals(
            startX = borderWidthPx,
            startY = borderWidthPx,
            width = size.width - borderWidthPx - borderWidthPx,
            height = size.height - borderWidthPx - borderWidthPx,
            color = expectedBackgroundColor
        )
}

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

/**
 * Gets the SemanticsNode's width if the provided [width] is null, and the SemanticsNode's height if the provided
 * [height] is null.
 *
 * @return A Pair containing `width to height`.
 */
private fun SemanticsNodeInteraction.getDimensionsIfNull(width: Int?, height: Int?): Pair<Int, Int> {
    val widthToUse: Int
    val heightToUse: Int
    if (width == null || height == null) {
        val size = fetchSemanticsNode().size
        widthToUse = width ?: size.width
        heightToUse = height ?: size.height
    } else {
        widthToUse = width
        heightToUse = height
    }
    return widthToUse to heightToUse
}
