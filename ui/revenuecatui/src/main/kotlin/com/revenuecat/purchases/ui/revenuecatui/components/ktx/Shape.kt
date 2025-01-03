@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Shape as RcShape

@JvmSynthetic
internal fun RcShape.toShape(): Shape =
    when (this) {
        is RcShape.Rectangle -> corners?.run {
            RoundedCornerShape(
                topStart = topLeading.dp,
                topEnd = topTrailing.dp,
                bottomEnd = bottomTrailing.dp,
                bottomStart = bottomLeading.dp,
            )
        } ?: RectangleShape
        is RcShape.Pill -> RoundedCornerShape(percent = 50)
    }

@JvmSynthetic
internal fun MaskShape.toShape(): Shape =
    when (this) {
        is MaskShape.Rectangle -> corners?.run {
            RoundedCornerShape(
                topStart = topLeading.dp,
                topEnd = topTrailing.dp,
                bottomEnd = bottomTrailing.dp,
                bottomStart = bottomLeading.dp,
            )
        } ?: RectangleShape

        is MaskShape.Pill -> RoundedCornerShape(percent = 50)
        is MaskShape.Concave -> GenericShape { size, _ ->
            // TODO Actually implement a Concave shape.
            // This is just a rectangle.
            lineTo(x = size.width, y = 0f)
            lineTo(x = size.width, y = size.height)
            lineTo(x = 0f, y = size.height)
            lineTo(x = 0f, y = 0f)
        }

        is MaskShape.Convex -> GenericShape { size, _ ->
            // TODO Actually implement a Convex shape.
            // This is just a rectangle.
            lineTo(x = size.width, y = 0f)
            lineTo(x = size.width, y = size.height)
            lineTo(x = 0f, y = size.height)
            lineTo(x = 0f, y = 0f)
        }
        is MaskShape.Circle -> CircleShape
    }
