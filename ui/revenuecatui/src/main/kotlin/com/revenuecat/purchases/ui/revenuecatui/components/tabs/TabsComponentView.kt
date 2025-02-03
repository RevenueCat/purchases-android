@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

@Composable
internal fun TabsComponentView(
    style: TabsComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Get a StackComponentState that calculates the overridden properties we should use.
    val tabsState = rememberUpdatedTabsComponentState(
        style = style,
        paywallState = state,
    )
    if (!tabsState.visible) return

    val backgroundColorStyle = tabsState.backgroundColor?.forCurrentTheme
    val borderStyle = tabsState.border?.let { rememberBorderStyle(border = it) }
    val shadowStyle = tabsState.shadow?.let { rememberShadowStyle(shadow = it) }

    AnimatedContent(
        targetState = state.selectedTabIndex,
        modifier = modifier
            .padding(tabsState.margin)
            .applyIfNotNull(shadowStyle) { shadow(it, tabsState.shape) }
            .applyIfNotNull(backgroundColorStyle) { background(it, tabsState.shape) }
            .clip(tabsState.shape)
            .applyIfNotNull(borderStyle) { border(it, tabsState.shape).padding(it.width) }
            .padding(tabsState.padding),
    ) { selectedTabIndex ->
        // Coerce it, just in case we get an out-of-range value.
        val tab = tabsState.tabs[selectedTabIndex.coerceIn(0..tabsState.tabs.lastIndex)]
        StackComponentView(
            style = tab.stack,
            state = state,
            clickHandler = clickHandler,
        )
    }
}
