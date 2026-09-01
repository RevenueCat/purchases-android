package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import dev.drewhamilton.poko.Poko

/**
 * What the user obtained while going through a checkpoint, delivered to
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint]'s callback once the checkpoint finishes.
 *
 * Invariants:
 * - [noWorkflowReason] is null exactly when a workflow was presented.
 * - [entitlements] and [virtualCurrencies] can only be non-empty when a workflow was presented.
 * - [error] can accompany a null [noWorkflowReason]: the workflow was presented but something failed inside it,
 *   like a purchase.
 */
@InternalRevenueCatAPI
@Poko
public class CheckpointGateResult internal constructor(
    /** The entitlements the user obtained during this checkpoint. Empty when they obtained none. */
    public val entitlements: List<EntitlementGrant>,
    /** The virtual currencies the user obtained during this checkpoint. Empty when they obtained none. */
    public val virtualCurrencies: List<VirtualCurrencyGrant>,
    /** Why no workflow was presented for this checkpoint, or null when one was. */
    public val noWorkflowReason: NoWorkflowReason?,
    /**
     * The failure that prevented a workflow from being presented (with [noWorkflowReason] equal to
     * [NoWorkflowReason.ERROR]), or that happened inside the presented workflow (with a null [noWorkflowReason]).
     */
    public val error: PurchasesError?,
) {

    /** The reason no workflow was presented for the checkpoint. */
    @Poko
    public class NoWorkflowReason internal constructor(internal val value: String) {

        override fun toString(): String = value

        public companion object {
            /** The checkpoint is configured, but no targeting rule matched. */
            @JvmField
            public val NO_MATCH: NoWorkflowReason = NoWorkflowReason("NO_MATCH")

            /** The customer was assigned to a holdout. */
            @JvmField
            public val HOLDOUT: NoWorkflowReason = NoWorkflowReason("HOLDOUT")

            /** The customer reached the configured frequency cap. */
            @JvmField
            public val FREQUENCY_CAPPED: NoWorkflowReason = NoWorkflowReason("FREQUENCY_CAPPED")

            /** The configuration needed to serve the checkpoint could not be read. */
            @JvmField
            public val CONFIGURATION_UNAVAILABLE: NoWorkflowReason = NoWorkflowReason("CONFIGURATION_UNAVAILABLE")

            /** The checkpoint identifier is not configured in the RevenueCat dashboard. */
            @JvmField
            public val UNKNOWN_CHECKPOINT: NoWorkflowReason = NoWorkflowReason("UNKNOWN_CHECKPOINT")

            /** The checkpoint identifier is invalid. */
            @JvmField
            public val INVALID_CHECKPOINT_IDENTIFIER: NoWorkflowReason =
                NoWorkflowReason("INVALID_CHECKPOINT_IDENTIFIER")

            /** A workflow should have been presented but failed to; [CheckpointGateResult.error] has the detail. */
            @JvmField
            public val ERROR: NoWorkflowReason = NoWorkflowReason("ERROR")
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
    /** How the user obtained the entitlement. */
    public val method: GrantMethod,
)

/**
 * A virtual currency amount the user obtained during a checkpoint.
 */
@InternalRevenueCatAPI
@Poko
public class VirtualCurrencyGrant internal constructor(
    /** The virtual currency code, as configured in the RevenueCat dashboard. */
    public val code: String,
    /** The amount of the virtual currency the user obtained. */
    public val amount: Int,
    /** How the user obtained the virtual currency. */
    public val method: GrantMethod,
)

/** How the user obtained an entitlement or virtual currency during a checkpoint. */
@InternalRevenueCatAPI
@Poko
public class GrantMethod internal constructor(internal val value: String) {

    override fun toString(): String = value

    public companion object {
        /** The user purchased a product that grants it. */
        @JvmField
        public val PURCHASED: GrantMethod = GrantMethod("PURCHASED")

        /** The user restored a purchase that grants it. */
        @JvmField
        public val RESTORED: GrantMethod = GrantMethod("RESTORED")
    }
}
