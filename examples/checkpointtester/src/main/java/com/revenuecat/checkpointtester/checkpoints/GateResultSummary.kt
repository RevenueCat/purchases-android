package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateResult

/**
 * One-line description of a gate result, shared by the screens that only need to display what happened.
 */
@OptIn(InternalRevenueCatAPI::class)
fun CheckpointGateResult.summary(): String {
    val error = error
    val noActionReason = noActionReason
    return when {
        entitlements.isNotEmpty() ->
            "Obtained ${entitlements.joinToString { it.identifier }}."
        error != null && noActionReason != null -> "Checkpoint failed: ${error.message}"
        error != null -> "Workflow finished with an error: ${error.message}"
        noActionReason != null -> "Nothing served ($noActionReason)."
        else -> "Workflow finished without the user obtaining anything."
    }
}
