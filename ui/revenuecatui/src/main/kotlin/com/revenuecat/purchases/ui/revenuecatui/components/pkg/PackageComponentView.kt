@file:JvmSynthetic
@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPaywallControlInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPackageRowSelection

@JvmSynthetic
@Composable
internal fun PackageComponentView(
    style: PackageComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controlInteractionTracker = LocalPaywallControlInteractionTracker.current
    StackComponentView(
        style = style.stackComponentStyle,
        state = state,
        clickHandler = { action ->
            // If this package is selectable, a click will select it. No need to pass the click to the clickHandler.
            if (!style.isSelectable) clickHandler(action)
        },
        modifier = modifier.conditional(style.isSelectable) {
            clickable(
                enabled = state.selectedPackageInfo?.uniqueId != style.uniqueId,
            ) {
                controlInteractionTracker.track(
                    paywallPackageRowSelection(
                        componentName = style.componentName,
                        destination = style.rcPackage,
                        origin = state.selectedPackageInfo?.rcPackage,
                        defaultPackage = state.defaultPackageForPackageRowAnalytics(),
                    ),
                )
                state.update(selectedPackageUniqueId = style.uniqueId)
            }
        },
    )
}
