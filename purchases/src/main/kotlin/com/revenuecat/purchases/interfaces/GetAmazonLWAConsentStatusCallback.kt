package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when making calls to sync attributes and offerings.
 */
public interface GetAmazonLWAConsentStatusCallback {
    /**
     * Called when the consent status was successfully fetched.
     *
     * @param consentStatus
     */
    public fun onSuccess(consentStatus: AmazonLWAConsentStatus)

    /**
     * Called when there was an error fetching the consent status.
     *
     * @param error A [PurchasesError]
     */
    public fun onError(error: PurchasesError)
}
