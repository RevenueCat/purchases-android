package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

internal object UIConstant {
    val defaultHorizontalPadding = 12.dp
    val defaultVerticalSpacing = 12.dp

    val footerRoundedBorderHeight = 12.dp

    val defaultPackageCornerRadius = 16.dp
    val defaultPackageBorderWidth = 1.5.dp

    const val purchaseInProgressButtonOpacity = 0.4f

    const val defaultAnimationDurationMillis = 200

    fun <T> defaultAnimation() = tween<T>(
        durationMillis = 200,
        easing = LinearOutSlowInEasing,
    )
}
