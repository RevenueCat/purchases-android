package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

@Composable
internal fun ConsistentPackageContentView(
    state: PaywallState.Loaded,
    creator: @Composable (TemplateConfiguration.PackageInfo) -> Unit,
) {
    ConsistentPackageContentView(
        packages = state.templateConfiguration.packages.all,
        selected = state.selectedPackage.value,
        creator = creator,
    )
}

/**
 * A wrapper composable that can display content based on a selected package
 * and maintain a consistent layout when that selected package changes.
 */
@Composable
internal fun ConsistentPackageContentView(
    packages: List<TemplateConfiguration.PackageInfo>,
    selected: TemplateConfiguration.PackageInfo,
    shouldAnimate: Boolean = true,
    creator: @Composable (TemplateConfiguration.PackageInfo) -> Unit,
) {
    Box {
        packages.forEach { packageItem ->
            val opacity = if (shouldAnimate) {
                animateFloatAsState(
                    targetValue = if (packageItem.rcPackage == selected.rcPackage) 1.0f else 0.0f,
                    animationSpec = UIConstant.defaultAnimation(),
                    label = "ConsistentPackageContentView",
                ).value
            } else {
                if (packageItem.rcPackage == selected.rcPackage) 1.0f else 0.0f
            }

            Box(
                modifier = Modifier
                    .alpha(opacity)
                    .align(Alignment.Center),
            ) {
                creator(packageItem)
            }
        }
    }
}
