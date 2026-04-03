@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@JvmSynthetic
@Composable
internal fun PackageComponentView(
    style: PackageComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val packageState = rememberUpdatedPackageComponentState(style = style, paywallState = state)

    // Notify PaywallState whenever this package's visibility changes so that purchase / pricing
    // state never references a hidden package. These effects run before the early-return so they
    // remain active even while invisible.
    LaunchedEffect(packageState.visible) {
        state.setPackageVisible(uniqueId = style.uniqueId, isVisible = packageState.visible)
    }
    DisposableEffect(style.uniqueId) {
        onDispose {
            // Clear rather than mark-false so that packages leaving one tab (and potentially
            // re-entering in another) are treated as unknown rather than hidden, preventing
            // reconcileSelectedIfHidden from evicting the selection prematurely.
            state.clearPackageVisible(uniqueId = style.uniqueId)
        }
    }

    if (!packageState.visible) return

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
            ) { state.update(selectedPackageUniqueId = style.uniqueId) }
        },
    )
}
