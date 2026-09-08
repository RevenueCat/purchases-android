package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko

/**
 * What a checkpoint evaluated to, delivered to [CheckpointListener.onCheckpointEvaluated] before anything is
 * presented.
 */
@InternalRevenueCatAPI
public abstract class CheckpointEvaluation internal constructor() {

    /** A rule matched and a flow is about to be presented. */
    public object MatchedFlow : CheckpointEvaluation() {
        override fun toString(): String = "MatchedFlow"
    }

    /** Nothing is served for this checkpoint; [reason] says why. */
    @Poko
    public class NoAction internal constructor(
        public val reason: CheckpointResult.NoAction.Reason,
    ) : CheckpointEvaluation()
}
