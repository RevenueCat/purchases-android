package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

@Composable
internal fun ConsistentPackageContentView(
    state: PaywallState.Loaded.Legacy,
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
    creator: @Composable (TemplateConfiguration.PackageInfo) -> Unit,
) {
    Box {
        packages.forEach { packageItem ->
            Box(
                modifier = Modifier
                    .alpha(0f)
                    .align(Alignment.Center),
            ) {
                creator(packageItem)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center),
        ) {
            creator(selected)
        }
    }
}
