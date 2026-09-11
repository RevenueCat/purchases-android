package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import dev.drewhamilton.poko.Poko

/**
 * Terminal outcome of a checkpoint-presented flow, kept on the [CheckpointRun] it belongs to.
 */
internal abstract class CheckpointFlowOutcome {

    /** The flow was dismissed without a purchase, restore, or error. */
    object Dismissed : CheckpointFlowOutcome() {
        override fun toString(): String = "Dismissed"
    }

    /** A purchase completed from the flow. */
    @Poko
    class Purchased(
        val customerInfo: CustomerInfo,
        val storeTransaction: StoreTransaction,
    ) : CheckpointFlowOutcome()

    /** Purchases were restored from the flow. */
    @Poko
    class Restored(
        val customerInfo: CustomerInfo,
    ) : CheckpointFlowOutcome()

    /**
     * A purchase or restore failed with [error], or the flow could not be kept on screen (e.g. a failed
     * re-present after a configuration change). Cancellations are reported as [Dismissed] instead.
     */
    @Poko
    class Error(
        val error: PurchasesError,
    ) : CheckpointFlowOutcome()

    /**
     * The user tapped a web checkout call to action and left to pay externally. There is no in-app completion
     * signal for that payment.
     */
    object WebCheckoutOpened : CheckpointFlowOutcome() {
        override fun toString(): String = "WebCheckoutOpened"
    }
}
