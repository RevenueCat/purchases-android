package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import dev.drewhamilton.poko.Poko

/**
 * Information delivered to [CheckpointListener.onCheckpointHit].
 */
@InternalRevenueCatAPI
@Poko
public class OnCheckpointHitInfo internal constructor(
    /** The identifier of the checkpoint that was hit. */
    public val identifier: String,
    /** The custom variables supplied when the checkpoint was hit. */
    public val customVariables: Map<String, CustomVariableValue>,
)

/**
 * Information delivered to [CheckpointListener.onCheckpointCompleted].
 */
@InternalRevenueCatAPI
@Poko
public class OnCheckpointCompletedInfo internal constructor(
    /** The identifier of the checkpoint that completed. */
    public val identifier: String,
    /** The custom variables supplied when the checkpoint was hit. */
    public val customVariables: Map<String, CustomVariableValue>,
    /** What the checkpoint resolved to. */
    public val result: CheckpointResult,
)

/**
 * Global listener for checkpoint activity, set through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener].
 * All methods are called on the main thread.
 */
@InternalRevenueCatAPI
public interface CheckpointListener {

    /** A checkpoint was hit, before evaluation. */
    public fun onCheckpointHit(hit: OnCheckpointHitInfo) {
        // Default empty implementation
    }

    /** The checkpoint completed and the result was returned. */
    public fun onCheckpointCompleted(completion: OnCheckpointCompletedInfo) {
        // Default empty implementation
    }
}
