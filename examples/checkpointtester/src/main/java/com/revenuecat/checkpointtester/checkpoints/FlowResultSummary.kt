package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult

/**
 * One-line description of a checkpoint result, shared by the screens that only need to display what happened.
 * A null flow means nothing was presented; the logs have the reason.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
fun CheckpointResult.summary(): String {
    val flow = flow ?: return "Nothing presented."
    return if (flow.obtainedEntitlements.isNotEmpty()) {
        "Obtained ${flow.obtainedEntitlements.joinToString { it.entitlementInfo.identifier }}."
    } else {
        "Flow finished without the user obtaining anything."
    }
}
