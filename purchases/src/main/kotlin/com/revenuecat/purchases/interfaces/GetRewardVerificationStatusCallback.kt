package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.RewardVerificationStatus

/**
 * Interface to be implemented when making calls that return a [RewardVerificationStatus].
 */
internal interface GetRewardVerificationStatusCallback {
    /**
     * Called when the verification status has been fetched successfully.
     */
    fun onReceived(status: RewardVerificationStatus)

    /**
     * Called after the request fails.
     */
    fun onError(error: PurchasesError)
}
