package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

/**
 * A wrapper composable that can display content based on a selected package
 * and maintain a consistent layout when that selected package changes.
 */
@Composable
internal fun ConsistentPackageContentView(
    packages: List<TemplateConfiguration.PackageInfo>,
    selected: TemplateConfiguration.PackageInfo,
    creator: @Composable (TemplateConfiguration.PackageInfo) -> Unit,
) {
    Box {
        packages.forEach { packageItem ->
            val opacity by animateFloatAsState(
                targetValue = if (packageItem.rcPackage == selected.rcPackage) 1.0f else 0.0f,
                animationSpec = UIConstant.defaultAnimation(),
                label = "ConsistentPackageContentView",
            )

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
