package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import dev.drewhamilton.poko.Poko

/**
 * Information about the checkpoint a [CheckpointListener] callback fires for.
 */
@InternalRevenueCatAPI
public abstract class CheckpointContext internal constructor() {

    /** The identifier of the checkpoint that was hit. */
    public abstract val identifier: String

    /** The custom variables supplied when the checkpoint was hit. */
    public abstract val customVariables: Map<String, CustomVariableValue>
}

/**
 * Context delivered to [CheckpointListener.onCheckpointHit].
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointHitContext internal constructor(
    override val identifier: String,
    override val customVariables: Map<String, CustomVariableValue>,
) : CheckpointContext()

/**
 * Context delivered to [CheckpointListener.onCheckpointCompleted].
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointCompletedContext internal constructor(
    override val identifier: String,
    override val customVariables: Map<String, CustomVariableValue>,
    /** What the checkpoint resolved to. */
    public val result: CheckpointResult,
) : CheckpointContext()

/**
 * Global listener for checkpoint activity, set through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener].
 * All methods are called on the main thread.
 */
@InternalRevenueCatAPI
public interface CheckpointListener {

    /** A checkpoint was hit, before evaluation. */
    public fun onCheckpointHit(context: CheckpointHitContext) {
        // Default empty implementation
    }

    /** The checkpoint completed and the result was returned. */
    public fun onCheckpointCompleted(context: CheckpointCompletedContext) {
        // Default empty implementation
    }
}
