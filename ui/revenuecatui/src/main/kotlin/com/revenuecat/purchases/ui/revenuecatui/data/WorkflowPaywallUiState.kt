package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Stable
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection

/**
 * Snapshot of the workflow-related state the UI needs to render a multi-step paywall.
 *
 * Non-null when the loaded paywall is a workflow; null otherwise. Updated atomically when the
 * user navigates between steps; [stepStates] grows lazily as steps are visited.
 */
@Stable
internal data class WorkflowPaywallUiState(
    val currentStepId: String,
    val stepStates: Map<String, PaywallState.Loaded.Components>,
    /**
     * Describes a navigation that should be animated. Set atomically with [currentStepId] so
     * the first recomposition after navigation already knows both surfaces and can position them
     * correctly — no gap-detection or [androidx.compose.animation.core.Animatable.snapTo] needed.
     *
     * Null when idle (no animation in progress or queued).
     */
    val pendingTransition: WorkflowPendingTransition? = null,
)

/**
 * Describes a navigation transition that is ready to be animated.
 *
 * @param fromStepId the step sliding out.
 * @param direction which way the new step enters.
 * @param id monotonically-increasing counter used as a [androidx.compose.runtime.key] discriminator.
 *   A fresh [androidx.compose.animation.core.Animatable] at 0f is created per distinct id, which
 *   means the first draw of each transition already starts at the correct offscreen position.
 */
internal data class WorkflowPendingTransition(
    val fromStepId: String,
    val direction: NavigationDirection,
    val id: Int,
)
