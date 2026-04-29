@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

private const val SLIDE_DURATION_MS = 350

// Keeps this many recent back-stack entries pre-composed so backward swipes feel instant.
// 3 covers typical A→B→C funnels without unbounded memory growth.
private const val BACK_STACK_WINDOW = 3

/**
 * Drives the cross-step slide animation for a workflow paywall.
 *
 * Owns:
 * - the set of step IDs currently composed in the slot table,
 * - the animation values used by [Modifier.workflowSlide] to position each step's content.
 *
 * Use [rememberWorkflowSlideState] to obtain an instance bound to the current workflow inputs.
 */
@Stable
internal class WorkflowSlideState(initialStepId: String) {

    /** Step IDs whose ComponentView is currently in the Compose slot table. */
    var composedStepIds: Set<String> by mutableStateOf(setOf(initialStepId))
        private set

    /**
     * Last step ID the [LaunchedEffect] has fully bound to its animation state.
     *
     * While `currentStepId != seenStepId`, we're in the recomposition triggered by the new
     * currentStepId, but the LaunchedEffect's `snapTo(0f)` hasn't run yet. We use this gap
     * to render with `progress = 0f` so the new step starts off-screen and the previous step
     * stays at position 0 — preventing the flash where the new step would otherwise appear
     * at its final position before the slide begins.
     */
    internal var seenStepId by mutableStateOf(initialStepId)
        private set

    internal var animatingFromStepId by mutableStateOf<String?>(null)
        private set

    internal var animatingDirection by mutableStateOf(NavigationDirection.NONE)
        private set

    // 1f = "fully arrived"; animation runs from 0f (off-screen) to 1f (on-screen).
    internal val animatable = Animatable(1f)

    internal suspend fun runTransition(
        toStepId: String,
        navigationDirection: NavigationDirection,
        backStack: List<String>,
        reachableStepIds: Set<String>,
        stepStates: Map<String, PaywallState.Loaded.Components>,
    ) {
        // Always include the target step (handles the synchronous fallback edge case).
        composedStepIds = composedStepIds + toStepId

        if (seenStepId != toStepId) {
            val fromId = seenStepId
            // Closing the gap before snapTo would cause the flash; we close it as part of
            // setting up the animation values below.
            seenStepId = toStepId

            if (navigationDirection != NavigationDirection.NONE) {
                animatingFromStepId = fromId
                animatingDirection = navigationDirection
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(SLIDE_DURATION_MS, easing = FastOutSlowInEasing),
                )
                animatingFromStepId = null
                animatingDirection = NavigationDirection.NONE
            }
        }

        evictAndPreWarm(
            currentStepId = toStepId,
            backStack = backStack,
            reachableStepIds = reachableStepIds,
            stepStates = stepStates,
        )
    }

    private suspend fun evictAndPreWarm(
        currentStepId: String,
        backStack: List<String>,
        reachableStepIds: Set<String>,
        stepStates: Map<String, PaywallState.Loaded.Components>,
    ) {
        val backStackWindow = backStack.takeLast(BACK_STACK_WINDOW).toSet()
        val targetIds = setOf(currentStepId) + reachableStepIds + backStackWindow
        composedStepIds = composedStepIds.intersect(targetIds)

        // Tier-2: stagger pre-warm of reachable/recent steps one per frame so each composition
        // pass stays small enough to avoid a visible hitch.
        val toPrewarm = (reachableStepIds + backStackWindow)
            .filter { it in stepStates && it !in composedStepIds }
        for (stepId in toPrewarm) {
            withFrameNanos {}
            composedStepIds = composedStepIds + stepId
        }
    }
}

@Composable
internal fun rememberWorkflowSlideState(
    workflowState: WorkflowPaywallUiState,
    navigationDirection: NavigationDirection,
): WorkflowSlideState {
    val state = remember { WorkflowSlideState(workflowState.currentStepId) }

    LaunchedEffect(workflowState.currentStepId) {
        state.runTransition(
            toStepId = workflowState.currentStepId,
            navigationDirection = navigationDirection,
            backStack = workflowState.backStack,
            reachableStepIds = workflowState.reachableStepIds,
            stepStates = workflowState.stepStates,
        )
    }

    return state
}

/**
 * Translates a step's content horizontally based on its role in the current slide animation:
 *
 * - **Current step**: slides from `±width` to `0` as the animation progresses.
 * - **Step being animated out**: slides from `0` to `∓width`.
 * - **All other (parked) steps**: positioned far off-screen so the GraphicsLayer skips drawing.
 *
 * State reads happen inside the layer block so animation frames invalidate the layer without
 * triggering recomposition. During the [WorkflowSlideState.seenStepId] / `currentStepId` gap,
 * progress is forced to 0 to keep the new step off-screen until the animation actually starts.
 */
internal fun Modifier.workflowSlide(
    state: WorkflowSlideState,
    stepId: String,
    currentStepId: String,
    navigationDirection: NavigationDirection,
): Modifier = graphicsLayer {
    val inGap = state.seenStepId != currentStepId
    val animatingFrom = if (inGap) state.seenStepId else state.animatingFromStepId
    val direction = if (inGap) navigationDirection else state.animatingDirection
    val progress = if (inGap) 0f else state.animatable.value
    val directionFactor = if (direction == NavigationDirection.FORWARD) 1f else -1f

    translationX = when (stepId) {
        currentStepId -> (1f - progress) * directionFactor * size.width
        animatingFrom -> -progress * directionFactor * size.width
        else -> 2f * size.width
    }
}
