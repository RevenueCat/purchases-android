
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.background
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import kotlin.math.roundToInt

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
            // The image is applied here as a draw-only background. Color overlays (if present) are
            // rendered separately in WithOptionalBackgroundOverlay to ensure the overlay covers the
            // full container, not just the image bounds. This matches the web builder behavior.
            //
            // We intentionally do NOT use Modifier.paint(): that modifier participates in layout and
            // imposes a size on the container (it sizes to the painter's scaled intrinsic size, or,
            // with sizeToIntrinsics = false and bounded constraints, expands to fill the max). A
            // background must never change the size of the element it decorates, exactly like a color
            // background. Otherwise a wrap-content (Fit) sticky footer with an image background would
            // measure taller than its content, over-reserving scroll clearance and letting the main
            // content scroll off-screen behind a transparent footer.
            this.clip(shape)
                .drawImageBehind(
                    painter = background.painter,
                    contentScale = background.contentScale,
                    // Center the image so that when it's scaled to cover (Crop/fill) and overflows the
                    // container, it's cropped equally on all sides and the middle stays visible.
                    alignment = Alignment.Center,
                )
        is BackgroundStyle.Video ->
            // Video backgrounds are handled specially - they need to be rendered
            // in a Box behind the content, so we do nothing here
            this
    }

/**
 * Draws [painter] behind the content, scaled by [contentScale] and positioned by [alignment] within
 * the node's already-measured bounds, then draws the content on top. Unlike [Modifier.paint] this is
 * a pure draw modifier: it never contributes to layout, so the background can never resize the
 * element it decorates. The drawing logic mirrors `PainterModifier.draw`; clipping to the desired
 * shape is expected to be applied by the caller (see [background]).
 */
private fun Modifier.drawImageBehind(
    painter: Painter,
    contentScale: ContentScale,
    alignment: Alignment,
): Modifier =
    this.drawWithContent {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.isSpecified && intrinsicSize.width.isFinite()) {
            intrinsicSize.width
        } else {
            size.width
        }
        val srcHeight = if (intrinsicSize.isSpecified && intrinsicSize.height.isFinite()) {
            intrinsicSize.height
        } else {
            size.height
        }
        val srcSize = Size(srcWidth, srcHeight)

        val scaledSize = if (size.width != 0f && size.height != 0f) {
            srcSize * contentScale.computeScaleFactor(srcSize, size)
        } else {
            Size.Zero
        }

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection,
        )

        translate(alignedPosition.x.toFloat(), alignedPosition.y.toFloat()) {
            with(painter) {
                draw(size = scaledSize)
            }
        }

        drawContent()
    }
