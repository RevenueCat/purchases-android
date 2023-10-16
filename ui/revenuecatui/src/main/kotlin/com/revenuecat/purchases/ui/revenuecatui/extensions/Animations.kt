package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

@Composable
internal fun PaywallViewModel.packageButtonActionInProgressOpacityAnimation(): Float {
    val alpha by animateFloatAsState(
        targetValue = if (actionInProgress.value) UIConstant.purchaseInProgressButtonOpacity else 1f,
        animationSpec = UIConstant.defaultAnimation(),
        label = "Package button action in progress alpha",
    )

    return alpha
}

@Composable
internal fun PaywallState.Loaded.packageButtonColorAnimation(
    packageInfo: TemplateConfiguration.PackageInfo,
    selectedColor: Color,
    unselectedColor: Color,
): Color {
    val isSelected = packageInfo == selectedPackage.value

    val result by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = UIConstant.defaultAnimation(),
        label = "Package button selected color: ${packageInfo.rcPackage.identifier}",
    )
    return result
}
