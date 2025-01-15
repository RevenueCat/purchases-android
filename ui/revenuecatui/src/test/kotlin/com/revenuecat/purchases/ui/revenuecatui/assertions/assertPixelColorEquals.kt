package com.revenuecat.purchases.ui.revenuecatui.assertions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat
import kotlin.math.abs

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
 * Assert the percentage of pixels in a rectangular area of this Composable that have a color whose individual color
 * channels satisfy the provided [red], [green], [blue] and [alpha] predicates.
 *
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 *
 * @param red Predicate for the red color channel. Return true if this color should be considered.
 * @param green Predicate for the green color channel. Return true if this color should be considered.
 * @param blue Predicate for the blue color channel. Return true if this color should be considered.
 * @param alpha Predicate for the alpha color channel. Return true if this color should be considered.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 * @param predicate The assertion you want to run. The `percentage` parameter is in range 0..100.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertPixelColorChannelPercentage(
    red: ((Float) -> Boolean)? = null,
    green: ((Float) -> Boolean)? = null,
    blue: ((Float) -> Boolean)? = null,
    alpha: ((Float) -> Boolean)? = null,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
    predicate: (percentage: Float) -> Boolean,
): SemanticsNodeInteraction {
    require(red != null || green != null || blue != null || alpha != null) {
        "At least one of `red`, `green`, `blue` or `alpha` should be non-null."
    }
    
    val (widthToUse, heightToUse) = getDimensionsIfNull(width = width, height = height)
    val pixels = readPixels(
        startX = startX,
        startY = startY,
        width = widthToUse,
        height = heightToUse,
    )

    return assert(
        SemanticsMatcher("Assert percentage of pixel color channels") {
            val count = pixels
                .groupBy { color -> Color(color) }
                .mapValues { (_, pixels) -> pixels.count() }
                .filterKeys { color ->
                    red?.invoke(color.red) ?: true &&
                        green?.invoke(color.green) ?: true &&
                        blue?.invoke(color.blue) ?: true &&
                        alpha?.invoke(color.alpha) ?: true
                }
                .values
                .sum()

            val percentage = count.toFloat() / (widthToUse * heightToUse)

            predicate(percentage)
        }
    )
}

/**
 * Assert the percentage of pixels in a rectangular area of this Composable that have a color approximately equal to
 * the provided [color].
 *
 * When running on the JVM, make sure your test class or function has the following annotations. `sdk` has to be >= 26.
 *
 * ```kotlin
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
 * ```
 *
 * @param color The color to assert.
 * @param threshold The maximum allowed absolute difference per color channel, for a color to be considered approximate.
 * @param startX The x-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param startY The y-coordinate of the first pixel to read from the Composable. Defaults to 0.
 * @param width The number of pixels to read from each row. Will read the entire row if this parameter is null.
 * @param height The number of rows to read. Will read all rows if this parameter is null.
 * @param predicate The assertion you want to run. The `percentage` parameter is in range 0..100.
 */
@Suppress("LongParameterList")
internal fun SemanticsNodeInteraction.assertApproximatePixelColorPercentage(
    color: Color,
    threshold: Float,
    startX: Int = 0,
    startY: Int = 0,
    width: Int? = null,
    height: Int? = null,
    predicate: (percentage: Float) -> Boolean,
): SemanticsNodeInteraction =
    assertPixelColorChannelPercentage(
        red = { abs(color.red - it) <= threshold },
        green = { abs(color.green - it) <= threshold },
        blue = { abs(color.blue - it) <= threshold },
        alpha = { abs(color.alpha - it) <= threshold },
        startX = startX,
        startY = startY,
        width = width,
        height = height,
        predicate = predicate
    )

/**
 * Asserts the border color of a rectangular element.
 *
 * @param size The size of the element. Will use the entire node's size if this is null.
 * @param offset The offset of the element within this node.
 */
internal fun SemanticsNodeInteraction.assertRectangularBorderColor(
    borderWidth: Dp,
    expectedBorderColor: Color,
    expectedBackgroundColor: Color,
    size: DpSize? = null,
    offset: DpOffset? = null,
): SemanticsNodeInteraction = run {

    val node = fetchSemanticsNode()
    // Compose seems to have a minimum border width of 2 px. See also Dp.Hairline.
    val borderWidthPx = with(node.layoutInfo.density) { borderWidth.roundToPx() }.coerceAtLeast(2)
    val sizePx = size?.let {
            with(node.layoutInfo.density) { IntSize(width = it.width.roundToPx(), height = it.height.roundToPx()) }
        } ?: node.size
    val offsetPx = offset?.let {
            with(node.layoutInfo.density) { IntOffset(x = it.x.roundToPx(), y = it.y.roundToPx()) }
    } ?: IntOffset(x = 0, y = 0)

    // Top edge
    assertPixelColorEquals(
        startX = offsetPx.x,
        startY = offsetPx.y,
        width = sizePx.width,
        height = borderWidthPx,
        color = expectedBorderColor
    )
        // Left edge
        .assertPixelColorEquals(
            startX = offsetPx.x,
            startY = offsetPx.y,
            width = borderWidthPx,
            height = sizePx.height,
            color = expectedBorderColor
        )
        // Right edge
        .assertPixelColorEquals(
            startX = offsetPx.x + sizePx.width - borderWidthPx,
            startY = offsetPx.y,
            width = borderWidthPx,
            height = sizePx.height,
            color = expectedBorderColor
        )
        // Bottom edge
        .assertPixelColorEquals(
            startX = offsetPx.x,
            startY = offsetPx.y + sizePx.height - borderWidthPx,
            width = sizePx.width,
            height = borderWidthPx,
            color = expectedBorderColor
        )
        // Inner area
        .assertPixelColorEquals(
            startX = offsetPx.x + borderWidthPx,
            startY = offsetPx.y + borderWidthPx,
            width = sizePx.width - borderWidthPx - borderWidthPx,
            height = sizePx.height - borderWidthPx - borderWidthPx,
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
