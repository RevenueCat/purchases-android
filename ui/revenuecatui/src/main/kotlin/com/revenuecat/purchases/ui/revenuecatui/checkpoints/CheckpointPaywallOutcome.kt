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

    /**
     * An app-owned presentation through [CheckpointOfferingPresenter] finished. Which store action, if any,
     * happened during it is not reported; [customerInfo] is the customer's information after syncing the store
     * purchases made during it.
     */
    @Poko
    public class Finished(
        public val customerInfo: CustomerInfo,
    ) : CheckpointPaywallOutcome()

    /**
     * A purchase or restore failed with [error], or the workflow could not be kept on screen (e.g. a failed
     * re-present after a configuration change). Cancellations are reported as [Dismissed] instead.
     */
    @Poko
    public class Error(
        public val error: PurchasesError,
    ) : CheckpointPaywallOutcome()

    /**
     * The user tapped a web checkout call to action and left to pay externally. There is no in-app completion
     * signal for that payment.
     */
    public object WebCheckoutOpened : CheckpointPaywallOutcome() {
        override fun toString(): String = "WebCheckoutOpened"
    }
}
