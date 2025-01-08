@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle

/**
 * Draws the [color] on top of the content.
 */
@JvmSynthetic
@Stable
internal fun Modifier.overlay(
    color: ColorStyle,
    shape: Shape = RectangleShape,
): Modifier = this then drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)

    onDrawWithContent {
        drawContent()
        when (color) {
            is ColorStyle.Solid -> drawOutline(outline, color.color)
            is ColorStyle.Gradient -> drawOutline(outline, color.brush)
        }
    }
}

/**
 * Draws the [color] behind the content.
 */
@JvmSynthetic
@Stable
internal fun Modifier.underlay(
    color: ColorStyle,
    shape: Shape = RectangleShape,
): Modifier = this then drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    when (color) {
        is ColorStyle.Solid -> drawOutline(outline, color.color)
        is ColorStyle.Gradient -> drawOutline(outline, color.brush)
    }
}
