package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import dev.drewhamilton.poko.Poko

/**
 * What a checkpoint evaluated to, delivered to [CheckpointListener.onCheckpointEvaluated] before anything is
 * presented.
 */
@InternalRevenueCatAPI
public abstract class CheckpointEvaluation internal constructor() {

    /** A rule matched and the checkpoint resolved to [offering], which is handed to the app to present. */
    @Poko
    public class MatchedOffering internal constructor(
        public val offering: Offering,
    ) : CheckpointEvaluation()

    /** A rule matched and a RevenueCat flow is about to be presented. */
    public object MatchedUIFlow : CheckpointEvaluation() {
        override fun toString(): String = "MatchedUIFlow"
    }

    /** Nothing is served for this checkpoint; [reason] says why. */
    @Poko
    public class NoAction internal constructor(
        public val reason: CheckpointResult.NoAction.Reason,
    ) : CheckpointEvaluation()
}
