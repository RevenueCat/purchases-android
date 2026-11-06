package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationPollStatus
import com.revenuecat.purchases.awaitGetRewardVerificationResult

@OptIn(InternalRevenueCatAPI::class)
internal fun interface RewardVerificationFetcher {
    suspend fun fetch(clientTransactionId: String, appUserID: String): RewardVerificationPollStatus

    companion object {
        fun forAdUnit(adUnitId: String?): RewardVerificationFetcher =
            RewardVerificationFetcher { clientTransactionId, appUserID ->
                Purchases.sharedInstance.awaitGetRewardVerificationResult(clientTransactionId, adUnitId, appUserID)
            }
    }
}
