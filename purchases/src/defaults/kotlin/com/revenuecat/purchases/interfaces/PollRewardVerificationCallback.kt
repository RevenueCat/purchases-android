package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult

/**
 * Callback for [com.revenuecat.purchases.Purchases.pollRewardVerification].
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun interface PollRewardVerificationCallback {
    /**
     * Called when polling completes, with the verified reward or a failed result.
     */
    public fun onCompleted(result: RewardVerificationResult)
}
