@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    additionalPadding: PaddingValues = PaddingValues(0.dp),
) {
    StackComponentView(style.stackComponentStyle, state, clickHandler, modifier, additionalPadding)
}
