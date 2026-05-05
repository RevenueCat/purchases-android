@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

/**
 * Snapshot of the data needed to render and animate the two-surface workflow paywall transition.
 *
 * Built during composition from [WorkflowPaywallUiState.pendingTransition], so the first frame
 * after navigation already has both surfaces positioned correctly — no gap-detection, no
 * [Animatable.snapTo], no [androidx.compose.runtime.withFrameNanos] required.
 *
 * Each subclass carries only the properties that are meaningful for its animation type, so callers
 * pattern-match rather than interrogating nullable fields that don't apply.
 *
 * @property visibleStepIds step IDs currently held in the Compose slot table.
 * @property animatingFromStepId step ID transitioning out; null when idle.
 * @property animatable drives progress 0f→1f. Created fresh via [key] for each new
 *   [WorkflowPaywallUiState.pendingTransition] so it always starts at 0f.
 */
internal sealed class WorkflowTransitionState {
    abstract val visibleStepIds: Set<String>
    abstract val animatingFromStepId: String?
    abstract val animatable: Animatable<Float, AnimationVector1D>

    /**
     * Both surfaces slide simultaneously: the incoming step translates in from one side while
     * the outgoing step translates out to the other. [animatingDirection] determines which sides.
     *
     * @property animatingDirection direction of the active transition; null when idle.
     */
    class SlideInOut(
        override val visibleStepIds: Set<String>,
        override val animatingFromStepId: String?,
        val animatingDirection: NavigationDirection?,
        override val animatable: Animatable<Float, AnimationVector1D>,
    ) : WorkflowTransitionState()

    companion object {
        const val SLIDE_DURATION_MS = 350
    }
}

/**
 * Builds a [WorkflowTransitionState] from [workflowState].
 *
 * When [WorkflowPaywallUiState.pendingTransition] is non-null:
 * - [key] on the transition id creates a fresh [Animatable] **at 0f during composition**, so
 *   the draw phase of Frame N (the first frame after navigation) already reads 0f. The incoming
 *   step is positioned offscreen and the outgoing step stays at 0 — all without a
 *   [Animatable.snapTo] call.
 * - [LaunchedEffect] only drives `animateTo(1f)`; it no longer sets up animation state.
 * - [onTransitionComplete] is called with the transition id when the animation finishes so
 *   the ViewModel can clear [WorkflowPaywallUiState.pendingTransition].
 *
 * @param transition animation type used for the transition. Defaults to
 *   [WorkflowTransitionAnimation.SlideInOut] for the existing horizontal slide behavior.
 */
@Composable
internal fun rememberWorkflowTransitionState(
    workflowState: WorkflowPaywallUiState,
    onTransitionComplete: (transitionId: Int) -> Unit,
    transition: WorkflowTransitionAnimation = WorkflowTransitionAnimation.SlideInOut(),
): WorkflowTransitionState {
    val currentStepId = workflowState.currentStepId
    val pendingTransition = workflowState.pendingTransition

    // key() on the transition id causes a new Animatable to be created in composition
    // when a transition starts. The new Animatable starts at 0f (incoming step offscreen)
    // so Frame N's draw is already correct — no snapTo() needed.
    val animatable: Animatable<Float, AnimationVector1D> = key(pendingTransition?.id) {
        remember { Animatable(if (pendingTransition != null) 0f else 1f) }
    }

    // rememberUpdatedState ensures the effect always calls the latest lambda without
    // restarting the LaunchedEffect (which is keyed on transition id, not the lambda).
    val latestOnTransitionComplete by rememberUpdatedState(onTransitionComplete)

    LaunchedEffect(pendingTransition?.id) {
        if (pendingTransition != null) {
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = transition.toAnimationSpec(),
            )
            latestOnTransitionComplete(pendingTransition.id)
        }
    }

    val visibleStepIds = if (pendingTransition != null) {
        setOf(pendingTransition.fromStepId, currentStepId)
    } else {
        setOf(currentStepId)
    }

    return when (transition) {
        is WorkflowTransitionAnimation.SlideInOut -> WorkflowTransitionState.SlideInOut(
            visibleStepIds = visibleStepIds,
            animatingFromStepId = pendingTransition?.fromStepId,
            animatingDirection = pendingTransition?.direction,
            animatable = animatable,
        )
    }
}

/**
 * Applies the visual transform for the active [WorkflowTransitionState] to a step's content.
 *
 * For [WorkflowTransitionState.SlideInOut], translates horizontally:
 * - **Current step**: slides from `±width` to `0` as progress goes 0→1.
 * - **Outgoing step**: slides from `0` to `∓width` as progress goes 0→1.
 * - **Other (parked) steps**: positioned far off-screen so the GraphicsLayer skips drawing.
 *
 * State reads happen inside the layer block so animation frames invalidate the layer without
 * triggering recomposition.
 */
internal fun Modifier.workflowTransition(
    state: WorkflowTransitionState,
    stepId: String,
    currentStepId: String,
): Modifier = graphicsLayer {
    val progress = state.animatable.value
    when (state) {
        is WorkflowTransitionState.SlideInOut -> {
            val directionFactor = if (state.animatingDirection == NavigationDirection.FORWARD) 1f else -1f
            translationX = when (stepId) {
                currentStepId -> (1f - progress) * directionFactor * size.width
                state.animatingFromStepId -> -progress * directionFactor * size.width
                else -> 2f * size.width
            }
        }
    }
}

private fun WorkflowTransitionAnimation.toAnimationSpec(): AnimationSpec<Float> = when (this) {
    is WorkflowTransitionAnimation.SlideInOut -> tween(durationMs, easing = easing)
}
