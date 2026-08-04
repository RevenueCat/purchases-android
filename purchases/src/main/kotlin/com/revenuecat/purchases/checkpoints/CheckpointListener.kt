package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Global listener for checkpoint activity, set through [com.revenuecat.purchases.Purchases.checkpointListener].
 * All methods are called on the main thread.
 */
@InternalRevenueCatAPI
public interface CheckpointListener {

    /** A checkpoint was reached, before evaluation. */
    public fun onCheckpointRegistered(checkpoint: CheckpointInfo) {
        // Default empty implementation
    }

    /** The outcome was decided. Same result the call site receives. */
    public fun onCheckpointResolved(checkpoint: CheckpointInfo, result: CheckpointResult) {
        // Default empty implementation
    }

    /** A checkpoint-presented experience finished, with its disposition. */
    public fun onCheckpointUIFinished(checkpoint: CheckpointInfo, uiResult: CheckpointUIResult) {
        // Default empty implementation
    }
}
