package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.awaitGetAdRewards
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class ConfiguredAdRewardsPurchasesAPI {
    suspend fun check(purchases: Purchases, callback: GetAdRewardsCallback) {
        purchases.getAdRewards("unit", callback)
        purchases.getAdRewards("unit") { rewards: ConfiguredAdRewards? -> }
        val result: ConfiguredAdRewards? = purchases.awaitGetAdRewards("unit")
        val unavailable: Boolean = result == null
        val noReward: Boolean = result != null && result.reward == null
    }
}
