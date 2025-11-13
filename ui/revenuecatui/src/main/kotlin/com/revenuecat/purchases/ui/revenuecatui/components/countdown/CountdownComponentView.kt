@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.countdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.CountdownComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

@Suppress("CompositionLocalAllowlist")
internal val LocalCountdownTime = compositionLocalOf<CountdownTime?> { null }

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
    CompositionLocalProvider(LocalCountdownTime provides countdownState.countdownTime) {
        StackComponentView(
            style = stackStyle,
            state = state,
            clickHandler = onClick,
            modifier = modifier,
        )
    }
}
