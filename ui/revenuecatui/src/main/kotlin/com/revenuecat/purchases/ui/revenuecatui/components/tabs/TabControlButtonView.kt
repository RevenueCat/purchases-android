@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.paywallTabControlButtonSelection

@Composable
internal fun TabControlButtonView(
    style: TabControlButtonComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    StackComponentView(
        style = style.stack,
        state = state,
        // We act like a button, so we're handling the click already.
        clickHandler = { },
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
        onStackClick = onStackClick@{
            val ordered = style.tabIdsOrdered
            val destinationIndex = style.tabIndex
            val resolvedTabIndex = if (ordered.isNotEmpty()) {
                destinationIndex.coerceIn(0, ordered.lastIndex)
            } else {
                destinationIndex
            }
            if (ordered.isNotEmpty()) {
                val originIndex = state.selectedTabIndex.coerceIn(0, ordered.lastIndex)
                if (originIndex == resolvedTabIndex) {
                    state.update(selectedTabIndex = resolvedTabIndex)
                    return@onStackClick
                }
                val originTabId = ordered[originIndex]
                val destinationTabId = style.tabId
                componentInteractionTracker.track(
                    paywallTabControlButtonSelection(
                        tabsComponentName = style.tabsComponentName,
                        destinationTabId = destinationTabId,
                        originIndex = originIndex,
                        destinationIndex = resolvedTabIndex,
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
            state.update(selectedTabIndex = resolvedTabIndex)
        },
    )
}
