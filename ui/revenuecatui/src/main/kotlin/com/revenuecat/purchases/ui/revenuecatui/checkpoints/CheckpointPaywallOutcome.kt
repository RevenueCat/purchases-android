package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import dev.drewhamilton.poko.Poko

/**
 * Terminal result of a checkpoint-presented paywall. Delivered in [CheckpointResult.PaywallPresented].
 */
@InternalRevenueCatAPI
public abstract class CheckpointPaywallOutcome internal constructor() {

    /** The paywall was dismissed without a purchase, restore, or error. */
    public object Dismissed : CheckpointPaywallOutcome() {
        override fun toString(): String = "Dismissed"
    }

    /** A purchase completed from the paywall. */
    @Poko
    public class Purchased(
        /** The customer's information after the purchase. */
        public val customerInfo: CustomerInfo,
        /** The purchased transaction. */
        public val storeTransaction: StoreTransaction,
    ) : CheckpointPaywallOutcome()

    /** Purchases were restored from the paywall. */
    @Poko
    public class Restored(
        /** The customer's information after the restore. */
        public val customerInfo: CustomerInfo,
    ) : CheckpointPaywallOutcome()

    /** A purchase or restore failed with [error]. Cancellations are reported as [Dismissed] instead. */
    @Poko
    public class Error(
        public val error: PurchasesError,
    ) : CheckpointPaywallOutcome()

    /**
     * The user tapped a web checkout call to action and left to pay externally. There is no in-app completion
     * signal for that payment; a later purchase, restore, or error in the same presentation replaces this
     * outcome.
     */
    public object WebCheckoutOpened : CheckpointPaywallOutcome() {
        override fun toString(): String = "WebCheckoutOpened"
    }
}
