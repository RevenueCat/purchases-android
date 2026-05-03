@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Describes the animation used to transition between workflow paywall steps.
 *
 * The visual transform for each subclass lives in [Modifier.workflowSlide]; the animation
 * timing lives in [rememberWorkflowSlideState]. Adding a new transition type requires:
 *  1. Adding a new subclass here.
 *  2. Adding a `when` branch in [Modifier.workflowSlide] for the visual transform.
 *  3. Adding a `when` branch in [rememberWorkflowSlideState] for the animation spec.
 */
internal sealed class WorkflowTransitionAnimation {
    data class Slide(
        val durationMs: Int = WorkflowSlideState.SLIDE_DURATION_MS,
        val easing: Easing = FastOutSlowInEasing,
    ) : WorkflowTransitionAnimation()
}
