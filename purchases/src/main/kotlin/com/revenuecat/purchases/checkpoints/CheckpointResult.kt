package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko

@InternalRevenueCatAPI
public abstract class CheckpointResult internal constructor() {

    public abstract val checkpoint: CheckpointInfo

    /**
     * A checkpoint experience was presented and finished. The disposition of the experience is delivered
     * through [CheckpointListener.onCheckpointUIFinished].
     */
    @Poko
    public class UIPresented internal constructor(
        override val checkpoint: CheckpointInfo,
    ) : CheckpointResult()

    /**
     * Nothing is served for this checkpoint; the user continues uninterrupted.
     */
    @Poko
    public class NoAction internal constructor(
        override val checkpoint: CheckpointInfo,
        public val reason: Reason,
    ) : CheckpointResult() {

        @Poko
        public class Reason internal constructor(public val value: String) {

            public companion object {
                @JvmField
                public val NO_MATCH: Reason = Reason("NO_MATCH")

                @JvmField
                public val HOLDOUT: Reason = Reason("HOLDOUT")

                @JvmField
                public val FREQUENCY_CAPPED: Reason = Reason("FREQUENCY_CAPPED")

                @JvmField
                public val CONFIGURATION_UNAVAILABLE: Reason = Reason("CONFIGURATION_UNAVAILABLE")

                @JvmField
                public val DISABLED: Reason = Reason("DISABLED")
            }
        }
    }
}
