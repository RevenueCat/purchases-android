package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Receives the [CheckpointGateResult] of a [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint]
 * call.
 */
@InternalRevenueCatAPI
public fun interface CheckpointGateCallback {

    /** Called on the main thread, exactly once per checkpoint call, when the checkpoint finishes. */
    public fun onCheckpointFinished(gateResult: CheckpointGateResult)
}
