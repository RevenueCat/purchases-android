package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.runtime.Stable

/**
 * Snapshot of the workflow-related state the UI needs to render a multi-step paywall.
 *
 * Non-null when the loaded paywall is a workflow; null otherwise. Updated atomically when the
 * user navigates between steps and when the background pre-warm finishes computing additional
 * step states.
 */
@Stable
internal data class WorkflowPaywallUiState(
    val currentStepId: String,
    val stepStates: Map<String, PaywallState.Loaded.Components>,
    val backStack: List<String>,
    val reachableStepIds: Set<String>,
)
