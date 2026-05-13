package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.RewardVerificationResult

/**
 * Interface to be implemented when making calls that return a [RewardVerificationResult].
 */
@OptIn(InternalRevenueCatAPI::class)
internal interface GetRewardVerificationResultCallback {
    /**
     * Called when the reward verification result has been fetched successfully.
     */
    fun onReceived(result: RewardVerificationResult)

    /**
     * Called after the request fails.
     */
    fun onError(error: PurchasesError)
}
