@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.inputsinglechoice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.InputSingleChoiceComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@Composable
internal fun InputSingleChoiceComponentView(
    style: InputSingleChoiceComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    StackComponentView(
        style = style.stack,
        state = state,
        clickHandler = clickHandler,
        componentInteractionTracker = componentInteractionTracker,
        modifier = modifier,
    )
}
