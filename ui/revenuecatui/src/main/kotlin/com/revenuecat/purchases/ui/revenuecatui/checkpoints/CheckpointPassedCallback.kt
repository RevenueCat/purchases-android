package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * Receives the outcome of a [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint] call.
 */
@InviteOnlyCheckpointsAPI
public fun interface CheckpointPassedCallback {

    /**
     * Called on the main thread, at most once per checkpoint call, when the user passes the checkpoint.
     * [result]'s [CheckpointResult.flow] is null when there was nothing to present: the checkpoint is not
     * configured, or no rule matched. [CheckpointResult.customer] is the customer's information after the flow,
     * synced when it ended with a purchase or restore.
     * Not called when the checkpoint could not be served, the customer's information could not be read included
     * (the error is handed to the [ErrorPresenter] and the user stays where they were), when the user backs out of
     * the presented flow (system back, or a back action on a flow's first step), nor when the checkpoint resolved
     * to a flow while another checkpoint flow was already being presented: that earlier call is the one that
     * reports. Why nothing was presented is reported in the logs.
     */
    public fun onCheckpointPassed(result: CheckpointResult)
}
