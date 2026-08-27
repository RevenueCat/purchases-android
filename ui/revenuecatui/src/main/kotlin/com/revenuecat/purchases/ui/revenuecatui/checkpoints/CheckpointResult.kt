package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import dev.drewhamilton.poko.Poko

@InternalRevenueCatAPI
public abstract class CheckpointResult internal constructor() {

    public abstract val checkpoint: CheckpointInfo

    /**
     * The checkpoint selected [offering] without presenting RevenueCat-managed UI. The app decides how to use
     * the offering, including whether and where to present it.
     */
    @Poko
    public class ReceivedOffering internal constructor(
        override val checkpoint: CheckpointInfo,
        public val offering: Offering,
    ) : CheckpointResult()

    /**
     * A checkpoint-triggered paywall was presented and finished with [paywallOutcome].
     */
    @Poko
    public class PaywallPresented internal constructor(
        override val checkpoint: CheckpointInfo,
        public val paywallOutcome: CheckpointPaywallOutcome,
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

                @JvmField
                public val UNKNOWN_CHECKPOINT: Reason = Reason("UNKNOWN_CHECKPOINT")

                /** The checkpoint identifier is invalid. */
                @JvmField
                public val INVALID_CHECKPOINT_IDENTIFIER: Reason = Reason("INVALID_CHECKPOINT_IDENTIFIER")
            }
        }
    }
}
