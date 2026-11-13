@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.header

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.HeaderComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun HeaderComponentView(
    style: HeaderComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    ancestorScrollOrientations: Set<Orientation> = emptySet(),
) {
    val topInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    StackComponentView(
        // The header owns its top clearance, including when its stack is empty or fixed-height.
        style = if (style.firstItemIgnoresSafeArea) {
            style.stackComponentStyle
        } else {
            style.stackComponentStyle.copy(applyTopWindowInsets = false)
        },
        state = state,
        clickHandler = clickHandler,
        modifier = modifier.conditional(!style.firstItemIgnoresSafeArea) {
            windowInsetsPadding(topInsets)
        },
        ancestorScrollOrientations = ancestorScrollOrientations,
    )
}
