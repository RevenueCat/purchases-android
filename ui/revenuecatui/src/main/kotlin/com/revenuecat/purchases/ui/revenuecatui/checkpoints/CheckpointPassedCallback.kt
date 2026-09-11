package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Receives the outcome of a [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint] call.
 */
@InternalRevenueCatAPI
public fun interface CheckpointPassedCallback {

    /**
     * Called on the main thread, at most once per checkpoint call, when the user passes the checkpoint.
     * [result] is null when nothing was presented: no rule matched, nothing could be served, or presenting
     * failed. Not called when the user backs out of the presented flow (system back, or a back action on a
     * flow's first step), nor when the checkpoint resolved to a flow while another checkpoint flow was already
     * being presented: that earlier call is the one that reports. The reason nothing was presented and any
     * failure are reported in the logs.
     */
    public fun onCheckpointPassed(result: FlowResult?)
}
