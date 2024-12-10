@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
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
