package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Presents the offering a checkpoint resolves to with app-owned UI, set through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointOfferingPresenter]. When none is set, the
 * offering's configured paywall is presented instead, falling back to the default paywall.
 */
@InternalRevenueCatAPI
public fun interface CheckpointOfferingPresenter {

    /**
     * Called on the main thread when a checkpoint resolves to [offering]. Present it however the app wants and
     * report the terminal outcome through [completion]; the checkpoint stays unresolved until then.
     */
    public fun present(offering: Offering, completion: CheckpointOfferingCompletion)
}

/**
 * How a [CheckpointOfferingPresenter] reports its presentation's terminal outcome. Only the first report
 * counts; later reports, including reports for a checkpoint call that no longer exists, are ignored.
 */
@InternalRevenueCatAPI
public interface CheckpointOfferingCompletion {

    /** The presentation ended without a purchase, restore, or error. */
    public fun dismissed()

    /** A purchase completed during the presentation. */
    public fun purchased(customerInfo: CustomerInfo, storeTransaction: StoreTransaction)

    /** Purchases were restored during the presentation. */
    public fun restored(customerInfo: CustomerInfo)

    /** The presentation failed, or a purchase or restore inside it failed. */
    public fun failed(error: PurchasesError)
}
