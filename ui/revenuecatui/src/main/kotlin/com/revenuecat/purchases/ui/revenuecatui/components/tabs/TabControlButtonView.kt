@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.paywalls.events.PaywallControlType
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPaywallControlInteractionTracker

@Composable
internal fun TabControlButtonView(
    style: TabControlButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val controlInteractionTracker = LocalPaywallControlInteractionTracker.current
    StackComponentView(
        style = style.stack,
        state = state,
        // We act like a button, so we're handling the click already.
        clickHandler = { },
        modifier = modifier.clickable {
            val componentValue = style.tabButtonName ?: style.tabId
            controlInteractionTracker.track(
                componentType = PaywallControlType.TAB,
                componentName = style.tabsComponentName,
                componentValue = componentValue,
                componentUrl = null,
            )
            state.update(selectedTabIndex = style.tabIndex)
        },
    )
}
