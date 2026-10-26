package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.components.stack.rememberUpdatedStackComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

/**
 * Returns whether the root needs an outer vertical scroll modifier. A vertical stack already
 * scrolls itself, while an explicit NONE opts out of both scroll paths.
 */
internal fun shouldWrapMainContentInVerticalScroll(rootStack: ComponentStyle): Boolean {
    val stack = rootStack as? StackComponentStyle ?: return true
    return shouldWrapMainContentInVerticalScroll(stack.scrollOrientation, stack.scrollExplicitlyDisabled)
}

/** Resolve the root's current rules before choosing the outer scroll modifier. */
@Composable
internal fun shouldWrapMainContentInVerticalScroll(
    rootStack: ComponentStyle,
    paywallState: PaywallState.Loaded.Components,
): Boolean {
    val stack = rootStack as? StackComponentStyle ?: return true
    val resolvedStack = rememberUpdatedStackComponentState(stack, paywallState)
    return shouldWrapMainContentInVerticalScroll(
        resolvedStack.scrollOrientation,
        resolvedStack.scrollExplicitlyDisabled,
    )
}

private fun shouldWrapMainContentInVerticalScroll(
    scrollOrientation: Orientation?,
    scrollExplicitlyDisabled: Boolean,
): Boolean = scrollOrientation != Orientation.Vertical && !scrollExplicitlyDisabled
