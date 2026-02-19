@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.pkg

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
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
