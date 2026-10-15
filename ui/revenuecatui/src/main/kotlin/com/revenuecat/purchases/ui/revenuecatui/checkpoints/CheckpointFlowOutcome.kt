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
     * An app-owned presentation through [PaywallPresenter] finished with the user going through it.
     * [customerInfo] is the customer's information after syncing the store purchases made during it; what the
     * user obtained is read from it, whatever the app reported.
     */
    @Poko
    class Finished(
        val customerInfo: CustomerInfo,
    ) : CheckpointFlowOutcome()

    /**
     * An app-owned presentation through [PaywallPresenter] finished with the user going through it, but the sync
     * of the store purchases made during it failed. The error was presented, and the user goes on with nothing
     * known to have been obtained.
     */
    object SyncFailed : CheckpointFlowOutcome() {
        override fun toString(): String = "SyncFailed"
    }

    /**
     * The flow could not be shown or kept on screen: its content failed to load, or a re-present after a
     * configuration change failed. The checkpoint is not passed. A purchase or restore that failed is not an
     * outcome: the paywall stays up and how the user then leaves decides.
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
