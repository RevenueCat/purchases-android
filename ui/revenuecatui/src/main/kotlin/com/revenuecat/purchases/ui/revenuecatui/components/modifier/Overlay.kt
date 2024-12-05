@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle

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
