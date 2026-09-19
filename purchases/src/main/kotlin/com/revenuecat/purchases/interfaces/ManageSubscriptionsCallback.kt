package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError

/**
 * Callback for [Purchases.showManageSubscriptions].
 */
public interface ManageSubscriptionsCallback {
    /**
     * Called when the subscription management page was opened successfully.
     */
    public fun onSuccess()

    /**
     * Called when the subscription management page could not be opened.
     */
    public fun onError(error: PurchasesError)
}
