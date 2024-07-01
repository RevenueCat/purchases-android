package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object UIConstant {
    val defaultHorizontalPadding = 12.dp
    val defaultVerticalSpacing = 12.dp

    val defaultCornerRadius = 20.dp
    val defaultPackageCornerRadius = 16.dp
    val defaultPackageBorderWidth = 1.5.dp

    val defaultColorAnimation: AnimationSpec<Color> = tween(durationMillis = 300, easing = LinearEasing)

    const val purchaseInProgressButtonOpacity = 0.4f

    const val defaultAnimationDurationMillis = 200

    // Useful for providing equal Spacer priorities
    // But lower than 1.0f
    const val halfWeight = 0.5f

    val iconButtonSize = 48.dp

    fun <T> defaultAnimation() = tween<T>(
        durationMillis = defaultAnimationDurationMillis,
        easing = LinearOutSlowInEasing,
    )
}
