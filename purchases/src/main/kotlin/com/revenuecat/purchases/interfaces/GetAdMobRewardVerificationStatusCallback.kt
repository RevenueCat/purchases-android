package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.AdMobRewardVerificationStatus
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to be implemented when making calls that return an [AdMobRewardVerificationStatus].
 */
@InternalRevenueCatAPI
internal interface GetAdMobRewardVerificationStatusCallback {
    /**
     * Called when the verification status has been fetched successfully.
     */
    fun onReceived(status: AdMobRewardVerificationStatus)

    /**
     * Called after the request fails.
     */
    fun onError(error: PurchasesError)
}
