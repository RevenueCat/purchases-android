package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import dev.drewhamilton.poko.Poko

/**
 * What the user obtained while going through a checkpoint, delivered to
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint]'s callback once the checkpoint finishes.
 *
 * Invariants:
 * - [noActionReason] is null exactly when a flow was presented.
 * - [entitlements] can only be non-empty when a flow was presented.
 * - [error] can accompany a null [noActionReason]: the flow was presented but something failed inside it,
 *   like a purchase.
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointGateResult internal constructor(
    /** The entitlements the user obtained during this checkpoint. Empty when they obtained none. */
    public val entitlements: List<EntitlementGrant>,
    /** Why nothing was presented for this checkpoint, or null when something was. */
    public val noActionReason: NoActionReason?,
    /**
     * The failure that prevented a flow from being presented (with [noActionReason] equal to
     * [NoActionReason.ERROR]), or that happened inside the presented flow (with a null [noActionReason]).
     */
    public val error: PurchasesError?,
) {

    /** The reason nothing was presented for the checkpoint. */
    @Poko
    public class NoActionReason internal constructor(internal val value: String) {

        override fun toString(): String = value

        public companion object {
            /** The checkpoint is configured, but no targeting rule matched. */
            @JvmField
            public val NO_MATCH: NoActionReason = NoActionReason("NO_MATCH")

            /** The customer was assigned to a holdout. */
            @JvmField
            public val HOLDOUT: NoActionReason = NoActionReason("HOLDOUT")

            /** The customer reached the configured frequency cap. */
            @JvmField
            public val FREQUENCY_CAPPED: NoActionReason = NoActionReason("FREQUENCY_CAPPED")

            /** The configuration needed to serve the checkpoint could not be read. */
            @JvmField
            public val CONFIGURATION_UNAVAILABLE: NoActionReason = NoActionReason("CONFIGURATION_UNAVAILABLE")

            /** The checkpoint identifier is not configured in the RevenueCat dashboard. */
            @JvmField
            public val UNKNOWN_CHECKPOINT: NoActionReason = NoActionReason("UNKNOWN_CHECKPOINT")

            /** The checkpoint identifier is invalid. */
            @JvmField
            public val INVALID_CHECKPOINT_IDENTIFIER: NoActionReason =
                NoActionReason("INVALID_CHECKPOINT_IDENTIFIER")

            /** Something should have been presented but failed to; [CheckpointGateResult.error] has the detail. */
            @JvmField
            public val ERROR: NoActionReason = NoActionReason("ERROR")
        }
    }
}

/**
 * An entitlement the user obtained during a checkpoint.
 */
@InternalRevenueCatAPI
@Poko
public class EntitlementGrant internal constructor(
    /** The entitlement identifier, as configured in the RevenueCat dashboard. */
    public val identifier: String,
)
