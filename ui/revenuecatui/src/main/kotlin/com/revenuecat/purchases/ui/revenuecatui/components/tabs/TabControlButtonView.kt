@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
internal fun TabControlButtonView(
    style: TabControlButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    StackComponentView(
        style = style.stack,
        state = state,
        // We act like a button, so we're handling the click already.
        clickHandler = { state.update(selectedTabIndex = style.tabIndex) },
        modifier = modifier.clickable { },
    )
}
