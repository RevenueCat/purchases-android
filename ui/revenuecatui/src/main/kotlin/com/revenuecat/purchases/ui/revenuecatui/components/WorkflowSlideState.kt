@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
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

private const val SLIDE_DURATION_MS = 350

/**
 * Snapshot of the data needed to render and animate the two-surface workflow paywall slide.
 *
 * Built during composition from [WorkflowPaywallUiState.pendingTransition], so the first frame
 * after navigation already has both surfaces positioned correctly — no gap-detection, no
 * [Animatable.snapTo], no [androidx.compose.runtime.withFrameNanos] required.
 *
 * @param visibleStepIds step IDs currently held in the Compose slot table.
 * @param animatingFromStepId step ID sliding out; null when idle.
 * @param animatingDirection direction of the active slide; [NavigationDirection.NONE] when idle.
 * @param animatable drives progress 0f→1f. Created fresh via [key] for each new
 *   [WorkflowPaywallUiState.pendingTransition] so it always starts at 0f.
 */
internal class WorkflowSlideState(
    val visibleStepIds: Set<String>,
    val animatingFromStepId: String?,
    val animatingDirection: NavigationDirection,
    val animatable: Animatable<Float, AnimationVector1D>,
)

/**
 * Builds a [WorkflowSlideState] from [workflowState].
 *
 * When [WorkflowPaywallUiState.pendingTransition] is non-null:
 * - [key] on the transition id creates a fresh [Animatable] **at 0f during composition**, so
 *   the draw phase of Frame N (the first frame after navigation) already reads 0f. The incoming
 *   step is positioned offscreen and the outgoing step stays at 0 — all without a
 *   [Animatable.snapTo] call.
 * - [LaunchedEffect] only drives `animateTo(1f)`; it no longer sets up animation state.
 * - [onTransitionComplete] is called with the transition id when the animation finishes so
 *   the ViewModel can clear [WorkflowPaywallUiState.pendingTransition].
 */
@Composable
internal fun rememberWorkflowSlideState(
    workflowState: WorkflowPaywallUiState,
    onTransitionComplete: (transitionId: Int) -> Unit,
): WorkflowSlideState {
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
                animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
            )
            latestOnTransitionComplete(pendingTransition.id)
        }
    }

    return WorkflowSlideState(
        visibleStepIds = if (pendingTransition != null) {
            setOf(pendingTransition.fromStepId, currentStepId)
        } else {
            setOf(currentStepId)
        },
        animatingFromStepId = pendingTransition?.fromStepId,
        animatingDirection = pendingTransition?.direction ?: NavigationDirection.NONE,
        animatable = animatable,
    )
}

/**
 * Translates a step's content horizontally based on its role in the current slide animation:
 *
 * - **Current step**: slides from `±width` to `0` as progress goes 0→1.
 * - **Outgoing step**: slides from `0` to `∓width` as progress goes 0→1.
 * - **Other (parked) steps**: positioned far off-screen so the GraphicsLayer skips drawing.
 *
 * State reads happen inside the layer block so animation frames invalidate the layer without
 * triggering recomposition.
 */
internal fun Modifier.workflowSlide(
    state: WorkflowSlideState,
    stepId: String,
    currentStepId: String,
): Modifier = graphicsLayer {
    val progress = state.animatable.value
    val directionFactor = if (state.animatingDirection == NavigationDirection.FORWARD) 1f else -1f

    translationX = when (stepId) {
        currentStepId -> (1f - progress) * directionFactor * size.width
        state.animatingFromStepId -> -progress * directionFactor * size.width
        else -> 2f * size.width
    }
}
