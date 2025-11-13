@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Composable
internal fun CountdownComponentView(
    style: CountdownComponentStyle,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val countdownState = rememberCountdownState(style.date)

    val stackStyle = if (countdownState.hasEnded && style.endStackComponentStyle != null) {
        style.endStackComponentStyle
    } else {
        style.countdownStackComponentStyle
    }
    StackComponentView(
        style = stackStyle,
        state = state,
        clickHandler = onClick,
        modifier = modifier,
    )
}
