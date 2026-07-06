package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationPollStatus
import com.revenuecat.purchases.awaitGetRewardVerificationResult

@OptIn(InternalRevenueCatAPI::class)
internal fun interface RewardVerificationFetcher {
    suspend fun fetch(clientTransactionId: String): RewardVerificationPollStatus

    companion object {
        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        val default: RewardVerificationFetcher = RewardVerificationFetcher { clientTransactionId ->
            Purchases.sharedInstance.awaitGetRewardVerificationResult(clientTransactionId)
        }
    }
}
