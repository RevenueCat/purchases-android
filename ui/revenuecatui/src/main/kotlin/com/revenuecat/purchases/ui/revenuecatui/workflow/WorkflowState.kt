package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

internal sealed interface WorkflowState {
    object Loading : WorkflowState
    data class Error(val message: String) : WorkflowState
    data class Loaded(val paywallState: PaywallState.Loaded.Components) : WorkflowState
}
