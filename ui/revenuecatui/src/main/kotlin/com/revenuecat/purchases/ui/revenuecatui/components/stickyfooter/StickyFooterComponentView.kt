@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
internal fun StickyFooterComponentView(
    style: StickyFooterComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    StackComponentView(style.stackComponentStyle, state, clickHandler, modifier)
}
