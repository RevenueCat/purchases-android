package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Global listener for checkpoint activity, set through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener].
 * All methods are called on the main thread.
 */
@InternalRevenueCatAPI
public interface CheckpointListener {

    /** A checkpoint was hit, before evaluation. */
    public fun onCheckpointHit(checkpoint: CheckpointInfo) {
        // Default empty implementation
    }

    /** The checkpoint completed and the result was returned. */
    public fun onCheckpointCompleted(checkpoint: CheckpointInfo, result: CheckpointResult) {
        // Default empty implementation
    }
}
