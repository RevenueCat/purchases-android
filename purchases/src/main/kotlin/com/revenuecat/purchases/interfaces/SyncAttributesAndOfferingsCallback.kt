package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when making calls to sync attributes and offerings.
 */
interface SyncAttributesAndOfferingsCallback {
    /**
     * Will be called after a successful syncing attributes and fetching of offerings.
     *
     * @param offerings
     */
    fun onSuccess(offerings: Offerings)

    /**
     * Will be called after an error syncing attributes fetching offerings
     *
     * @param error A [PurchasesError] containing the reason for the failure when fetching offerings.
     */
    fun onError(error: PurchasesError)
}
