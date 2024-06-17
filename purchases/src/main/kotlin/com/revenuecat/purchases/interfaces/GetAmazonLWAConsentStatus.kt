package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when making calls to sync attributes and offerings.
 */
interface GetAmazonLWAConsentStatusCallback {
    /**
     * Called when the consent status was successfully fetched.
     *
     * @param consentStatus
     */
    fun onSuccess(consentStatus: AmazonLWAConsentStatus)

    /**
     * Called when there was an error fetching the consent status.
     *
     * @param error A [PurchasesError]
     */
    fun onError(error: PurchasesError)
}
