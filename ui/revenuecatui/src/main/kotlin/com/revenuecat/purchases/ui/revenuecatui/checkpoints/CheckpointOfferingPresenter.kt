package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering

/**
 * Presents the offering a checkpoint resolves to with app-owned UI, set through
 * [com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointOfferingPresenter]. When none is set, the
 * offering's configured paywall is presented instead, falling back to the default paywall.
 */
@InternalRevenueCatAPI
public fun interface CheckpointOfferingPresenter {

    /**
     * Called on the main thread when a checkpoint resolves to [offering]. Present it however the app wants and
     * report through [completion] once the presentation is over; the checkpoint stays unresolved until then.
     * The SDK works out what the user obtained by itself, so the app doesn't report purchases or restores.
     */
    public fun present(offering: Offering, completion: CheckpointOfferingCompletion)
}

/**
 * How a [CheckpointOfferingPresenter] reports that its presentation is over. Only the first report counts;
 * later reports, including reports for a checkpoint call that no longer exists, are ignored.
 */
@InternalRevenueCatAPI
public interface CheckpointOfferingCompletion {

    /**
     * The presentation is over, however it ended. The SDK syncs any store purchase made during it, refreshes the
     * customer's information, and resolves the checkpoint with what the user obtained. Purchases made through
     * the SDK or through the app's own billing client are both picked up; an app that disabled automatic
     * purchase syncing must call [com.revenuecat.purchases.Purchases.syncPurchases] before reporting.
     */
    public fun finished()

    /**
     * The offering could not be presented, or the presentation failed. The checkpoint resolves with an error
     * and nothing obtained.
     */
    public fun failed()
}
