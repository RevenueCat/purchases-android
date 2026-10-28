package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.awaitGetAdRewards
import kotlin.time.Duration

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class ConfiguredAdRewardsAPI {
    suspend fun check(purchases: Purchases, entitlement: AdRewardConfiguration.Entitlement) {
        purchases.getAdRewards("unit") { rewards ->
            if (rewards != null) {
                val primary: AdRewardConfiguration? = rewards.reward
                val additional: List<AdRewardConfiguration> = rewards.moreRewards
            }
        }
        val result: ConfiguredAdRewards? = purchases.awaitGetAdRewards("unit")
        val duration: Duration = entitlement.duration
        val seconds: Long = entitlement.durationInSeconds
        val unavailable: Boolean = result == null
        val noReward: Boolean = result != null && result.reward == null
    }
}
