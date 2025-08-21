package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import com.revenuecat.purchases.paywalls.components.PaywallAnimation
import com.revenuecat.purchases.paywalls.components.PaywallAnimation.AnimationType
import com.revenuecat.purchases.paywalls.components.PaywallTransition
import com.revenuecat.purchases.paywalls.components.PaywallTransition.TransitionType.Fade
import com.revenuecat.purchases.paywalls.components.PaywallTransition.TransitionType.FadeAndScale
import com.revenuecat.purchases.paywalls.components.PaywallTransition.TransitionType.Scale
import com.revenuecat.purchases.paywalls.components.PaywallTransition.TransitionType.Slide

@Composable
fun TransitionView(transition: PaywallTransition?, content: @Composable () -> Unit) {
    if (transition == null) {
        content()
    } else {
        if (transition.displacementStrategy == PaywallTransition.DisplacementStrategy.GREEDY) {
            Box {
                Box(modifier = Modifier.hidden()) {
                    content()
                }

                transition.AnimatedVisibility { content() }
            }
        } else {
            transition.AnimatedVisibility { content() }
        }
    }
}

private fun Modifier.hidden(): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
        // do not insert anything into the view
    }
}

@Composable
private fun PaywallTransition.AnimatedVisibility(content: @Composable () -> Unit) {
    var shouldShow by remember(this) { mutableStateOf(false) }
    LaunchedEffect(this) {
        shouldShow = true
    }
    AnimatedVisibility(
        visible = shouldShow,
        enter = enterTransition()
    ) {
        content()
    }
}

private fun PaywallTransition.enterTransition() = when (type) {
    is Fade -> fadeIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    )

    is FadeAndScale -> fadeIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    ) + scaleIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    )

    is Scale -> scaleIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    )

    is Slide -> slideIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    ) { IntOffset(-SensibleDefaults.X_OFFSET, 0) }

    else -> fadeIn(
        tween(
            animation?.msDuration ?: SensibleDefaults.DURATION,
            delayMillis = animation?.msDelay ?: SensibleDefaults.DELAY,
            easing = animation.easing()
        )
    )
}

private object SensibleDefaults {
    const val DURATION = 300
    const val DELAY = 0
    const val X_OFFSET = 180
}

private fun PaywallAnimation?.easing(): Easing = this?.getEasing() ?: LinearOutSlowInEasing
private fun PaywallAnimation.getEasing(): Easing = when (type) {
    is AnimationType.EaseIn -> FastOutSlowInEasing
    is AnimationType.EaseOut -> FastOutLinearInEasing
    is AnimationType.EaseInOut -> LinearOutSlowInEasing
    is AnimationType.Linear -> LinearEasing
    else -> LinearOutSlowInEasing
}