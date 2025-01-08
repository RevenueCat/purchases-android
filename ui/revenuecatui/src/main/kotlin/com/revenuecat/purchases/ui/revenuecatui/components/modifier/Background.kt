@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

@JvmSynthetic
@Stable
internal fun Modifier.background(
    color: ColorStyle,
    shape: Shape = RectangleShape,
): Modifier =
    when (color) {
        is ColorStyle.Solid -> this.background(color.color, shape)
        is ColorStyle.Gradient -> this.background(color.brush, shape, alpha = 1f)
    }

@JvmSynthetic
@Stable
internal fun Modifier.background(
    background: BackgroundStyle,
    shape: Shape = RectangleShape,
): Modifier =
    when (background) {
        is BackgroundStyle.Color -> this.background(color = background.color, shape = shape)
        is BackgroundStyle.Image ->
            this.paint(
                painter = background.painter,
                contentScale = background.contentScale,
            )
                .applyIfNotNull(background.colorOverlay) { underlay(it, shape) }
                .clip(shape)
    }
