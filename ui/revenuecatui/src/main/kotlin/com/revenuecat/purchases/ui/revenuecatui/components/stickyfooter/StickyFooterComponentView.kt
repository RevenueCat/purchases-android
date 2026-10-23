@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.resolvedFooterLayout
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@Composable
internal fun StickyFooterComponentView(
    style: StickyFooterComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
) {
    val layout = resolvedFooterLayout(state)
    if (layout == StickyFooterComponent.Layout.HIDDEN) return
    val split = layout == StickyFooterComponent.Layout.SPLIT_LEADING ||
        layout == StickyFooterComponent.Layout.SPLIT_TRAILING
    val stackStyle = style.stackComponentStyle
    val resolvedStackStyle = if (split) {
        stackStyle.copy(
            applyTopWindowInsets = true,
            scrollOrientation = stackStyle.scrollOrientation
                ?: if (stackStyle.scrollExplicitlyDisabled) null else Orientation.Vertical,
        )
    } else {
        stackStyle
    }
    StackComponentView(
        style = resolvedStackStyle,
        state = state,
        clickHandler = clickHandler,
        modifier = modifier,
        componentInteractionTracker = componentInteractionTracker,
    )
}
