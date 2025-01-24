@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Shape as RcShape

/**
 * The scale factor to use on the Y axis when drawing a Concave or Convex shape. This will be multiplied by Y to
 * determine the maximum distance of the curve to the bottom of the shape.
 */
private const val SCALE_Y_OFFSET_CONCAVE_CONVEX = 0.2f

@JvmSynthetic
internal fun RcShape.toShape(): Shape = cornerRadiuses.convertCornerRadiusesToShape()

@JvmSynthetic
internal fun MaskShape.toShape(): Shape =
    when (this) {
        is MaskShape.Rectangle -> corners?.convertCornerRadiusesToShape() ?: RectangleShape

        is MaskShape.Concave -> GenericShape { size, _ ->
            val yOffset = SCALE_Y_OFFSET_CONCAVE_CONVEX * size.height * 2f

            moveTo(x = 0f, y = 0f)
            lineTo(x = size.width, y = 0f)
            lineTo(x = size.width, y = size.height)
            quadraticTo(x1 = size.width / 2, y1 = size.height - yOffset, x2 = 0f, y2 = size.height)
            lineTo(x = 0f, y = 0f)
        }

        is MaskShape.Convex -> GenericShape { size, _ ->
            val yOffset = SCALE_Y_OFFSET_CONCAVE_CONVEX * size.height

            moveTo(x = 0f, y = 0f)
            lineTo(x = size.width, y = 0f)
            lineTo(x = size.width, y = size.height - yOffset)
            quadraticTo(x1 = size.width / 2, y1 = size.height + yOffset, x2 = 0f, y2 = size.height - yOffset)
            lineTo(x = 0f, y = 0f)
        }
        is MaskShape.Circle -> CircleShape
    }

private fun CornerRadiuses.convertCornerRadiusesToShape(): Shape = when (this) {
    is CornerRadiuses.Percentage -> RoundedCornerShape(
        topStartPercent = topLeading,
        topEndPercent = topTrailing,
        bottomEndPercent = bottomTrailing,
        bottomStartPercent = bottomLeading,
    )
    is CornerRadiuses.Dp -> RoundedCornerShape(
        topStart = topLeading.dp,
        topEnd = topTrailing.dp,
        bottomEnd = bottomTrailing.dp,
        bottomStart = bottomLeading.dp,
    )
}
