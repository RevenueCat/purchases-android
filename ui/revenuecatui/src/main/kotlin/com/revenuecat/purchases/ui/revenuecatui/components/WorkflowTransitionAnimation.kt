@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Describes the animation used to transition between workflow paywall steps.
 *
 * The visual transform for each subclass lives in [Modifier.workflowTransition]; the animation
 * timing lives in [rememberWorkflowTransitionState]. Adding a new transition type requires:
 *  1. Adding a new subclass here.
 *  2. Adding a matching [WorkflowTransitionState] subclass with its animation-specific properties.
 *  3. Adding a `when` branch in [rememberWorkflowTransitionState] to build the state subclass.
 *  4. Adding a `when` branch in [Modifier.workflowTransition] for the visual transform.
 */
internal sealed class WorkflowTransitionAnimation {
    data class SlideInOut(
        val durationMs: Int = WorkflowTransitionState.SLIDE_DURATION_MS,
        val easing: Easing = FastOutSlowInEasing,
    ) : WorkflowTransitionAnimation()
}
