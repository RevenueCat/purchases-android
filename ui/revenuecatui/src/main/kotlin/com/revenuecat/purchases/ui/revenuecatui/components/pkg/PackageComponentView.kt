@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.performSelectionHapticFeedback
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallPackageRowSelection

@JvmSynthetic
@Composable
internal fun PackageComponentView(
    style: PackageComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val packageState = rememberUpdatedPackageComponentState(style = style, paywallState = state)

    if (!packageState.visible) return

    val view = LocalView.current
    val selected = remember(state.selectedPackageInfo?.uniqueId, style.uniqueId) {
        state.selectedPackageInfo?.uniqueId == style.uniqueId
    }

    StackComponentView(
        style = style.stackComponentStyle,
        state = state,
        clickHandler = { action ->
            // If this package is selectable, a click will select it. No need to pass the click to the clickHandler.
            if (!style.isSelectable) clickHandler(action)
        },
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
        selected = if (style.isSelectable) selected else null,
        onStackClick = if (style.isSelectable) {
            click@{
                if (selected) return@click
                componentInteractionTracker.track(
                    paywallPackageRowSelection(
                        componentName = style.componentName,
                        destination = style.rcPackage,
                        origin = state.selectedPackageInfo?.rcPackage,
                        defaultPackage = state.defaultPackageForPackageRowAnalytics(),
                    ),
                )
                if (style.hapticFeedbackEnabled) view.performSelectionHapticFeedback()
                state.update(selectedPackageUniqueId = style.uniqueId)
            }
        } else {
            null
        },
    )
}
