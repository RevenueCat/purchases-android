package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult

/**
 * One-line description of a gate result, shared by the screens that only need to display what happened.
 */
@OptIn(InternalRevenueCatAPI::class)
fun CheckpointGateResult.summary(): String {
    val error = error
    val noWorkflowReason = noWorkflowReason
    return when {
        entitlements.isNotEmpty() ->
            "Obtained ${entitlements.joinToString { it.identifier }}."
        error != null && noWorkflowReason != null -> "Checkpoint failed: ${error.message}"
        error != null -> "Workflow finished with an error: ${error.message}"
        noWorkflowReason != null -> "No workflow served ($noWorkflowReason)."
        else -> "Workflow finished without the user obtaining anything."
    }
}
