@file:JvmSynthetic
@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallTabControlButtonSelection

@Composable
internal fun TabControlButtonView(
    style: TabControlButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
) {
    val componentInteractionTracker = LocalPaywallComponentInteractionTracker.current
    StackComponentView(
        style = style.stack,
        state = state,
        // We act like a button, so we're handling the click already.
        clickHandler = { },
        modifier = modifier.clickable {
            val ordered = style.tabIdsOrdered
            val destinationIndex = style.tabIndex
            if (ordered.isNotEmpty()) {
                val originIndex = state.selectedTabIndex.coerceIn(0, ordered.lastIndex)
                val coercedDestination = destinationIndex.coerceIn(0, ordered.lastIndex)
                if (originIndex == coercedDestination) {
                    state.update(selectedTabIndex = destinationIndex)
                    return@clickable
                }
                val originTabId = ordered[originIndex]
                val destinationTabId = style.tabId
                componentInteractionTracker.track(
                    paywallTabControlButtonSelection(
                        tabsComponentName = style.tabsComponentName,
                        destinationTabId = destinationTabId,
                        originIndex = originIndex,
                        destinationIndex = coercedDestination,
                        originContextName = style.tabContextNamesById[originTabId],
                        destinationContextName = style.tabContextNamesById[destinationTabId],
                        defaultIndex = style.tabsDefaultTabIndex,
                    ),
                )
            } else {
                componentInteractionTracker.track(
                    paywallTabControlButtonSelection(
                        tabsComponentName = style.tabsComponentName,
                        destinationTabId = style.tabId,
                        originIndex = null,
                        destinationIndex = null,
                        originContextName = null,
                        destinationContextName = null,
                        defaultIndex = null,
                    ),
                )
            }
            state.update(selectedTabIndex = destinationIndex)
        },
    )
}
