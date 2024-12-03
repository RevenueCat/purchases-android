@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.ui.revenuecatui.components.property.ColorStyle

@JvmSynthetic
@Stable
internal fun Modifier.background(
    color: ColorStyle,
    shape: Shape = RectangleShape,
): Modifier =
    when (color) {
        is ColorStyle.Solid -> background(color.color, shape)
        is ColorStyle.Gradient -> background(color.brush, shape, alpha = 1f)
    }
