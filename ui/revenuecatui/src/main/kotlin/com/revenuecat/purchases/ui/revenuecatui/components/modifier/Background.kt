
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle

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
            // The image is applied here via paint(). Color overlays (if present) are rendered
            // separately in WithOptionalBackgroundOverlay to ensure the overlay covers the full
            // container, not just the image bounds. This matches the web builder behavior.
            this.clip(shape)
                .paint(
                    painter = background.painter,
                    contentScale = background.contentScale,
                    alignment = Alignment.TopCenter,
                )
        is BackgroundStyle.Video ->
            // Video backgrounds are handled specially - they need to be rendered
            // in a Box behind the content, so we do nothing here
            this
    }
