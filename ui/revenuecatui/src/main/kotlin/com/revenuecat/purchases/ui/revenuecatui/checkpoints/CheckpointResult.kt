package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import dev.drewhamilton.poko.Poko

/**
 * What a checkpoint resolved to, returned by
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint].
 */
@InternalRevenueCatAPI
public abstract class CheckpointResult internal constructor() {

    /**
     * The checkpoint selected [offering] without presenting RevenueCat-managed UI. The app decides how to use
     * the offering, including whether and where to present it.
     *
     * Not currently produced: a matched offering is presented through the registered
     * [CheckpointOfferingPresenter], or through its own paywall falling back to the default paywall, and
     * resolves as [PaywallPresented].
     */
    @Poko
    public class ReceivedOffering internal constructor(
        public val offering: Offering,
    ) : CheckpointResult()

    /**
     * A checkpoint-triggered paywall was presented and finished with [paywallOutcome].
     */
    @Poko
    public class PaywallPresented internal constructor(
        public val paywallOutcome: CheckpointPaywallOutcome,
    ) : CheckpointResult()

    /**
     * Nothing is served for this checkpoint; the user continues uninterrupted.
     */
    @Poko
    public class NoAction internal constructor(
        public val reason: Reason,
    ) : CheckpointResult() {

        /** The reason nothing was served for the checkpoint. */
        @Poko
        public class Reason internal constructor(internal val value: String) {

            override fun toString(): String = value

            public companion object {
                /** The checkpoint is configured, but no targeting rule matched. */
                @JvmField
                public val NO_MATCH: Reason = Reason("NO_MATCH")

                /** The customer was assigned to a holdout. */
                @JvmField
                public val HOLDOUT: Reason = Reason("HOLDOUT")

                /** The customer reached the configured frequency cap. */
                @JvmField
                public val FREQUENCY_CAPPED: Reason = Reason("FREQUENCY_CAPPED")

                /** The configuration needed to serve the checkpoint could not be read. */
                @JvmField
                public val CONFIGURATION_UNAVAILABLE: Reason = Reason("CONFIGURATION_UNAVAILABLE")

                /** The checkpoint identifier is not configured in the RevenueCat dashboard. */
                @JvmField
                public val UNKNOWN_CHECKPOINT: Reason = Reason("UNKNOWN_CHECKPOINT")

                /** The checkpoint identifier is invalid. */
                @JvmField
                public val INVALID_CHECKPOINT_IDENTIFIER: Reason = Reason("INVALID_CHECKPOINT_IDENTIFIER")
            }
        }
    }
}
