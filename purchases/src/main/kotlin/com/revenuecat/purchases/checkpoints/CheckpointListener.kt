package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * Global listener for checkpoint activity, set through [com.revenuecat.purchases.Purchases.checkpointListener].
 * All methods are called on the main thread.
 */
@InternalRevenueCatAPI
public interface CheckpointListener {

    /** A checkpoint was hit, before evaluation. */
    public fun onCheckpointHit(checkpoint: CheckpointInfo) {
        // Default empty implementation
    }

    /** The outcome was decided. Same result the call site receives. */
    public fun onCheckpointResolved(checkpoint: CheckpointInfo, result: CheckpointResult) {
        // Default empty implementation
    }

    /** A checkpoint-presented paywall finished, with its result. */
    public fun onCheckpointPaywallFinished(checkpoint: CheckpointInfo, paywallOutcome: CheckpointPaywallOutcome) {
        // Default empty implementation
    }
}
