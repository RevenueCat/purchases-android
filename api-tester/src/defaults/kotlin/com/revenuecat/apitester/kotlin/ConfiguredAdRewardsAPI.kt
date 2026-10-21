package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.awaitGetAdRewards
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback
import kotlin.time.Duration

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class ConfiguredAdRewardsAPI {
    suspend fun check(purchases: Purchases, entitlement: AdRewardConfiguration.Entitlement) {
        purchases.getAdRewards(
            "unit",
            object : GetAdRewardsCallback {
                override fun onReceived(rewards: ConfiguredAdRewards) {
                    val primary: AdRewardConfiguration? = rewards.reward
                    val additional: List<AdRewardConfiguration> = rewards.moreRewards
                }

                override fun onError(error: PurchasesError) {
                    val details: String? = error.underlyingErrorMessage
                }
            },
        )
        val result: ConfiguredAdRewards = purchases.awaitGetAdRewards("unit")
        val duration: Duration = entitlement.duration
        val seconds: Long = entitlement.durationInSeconds
        val noReward: Boolean = result.reward == null
    }
}
