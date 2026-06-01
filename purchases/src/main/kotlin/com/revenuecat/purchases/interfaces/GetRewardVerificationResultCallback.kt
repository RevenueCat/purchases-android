package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationError
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
     * Called after the request fails. The error carries whether the failure was a transient
     * server error (see [RewardVerificationError.isServerError]).
     */
    fun onError(error: RewardVerificationError)
}
