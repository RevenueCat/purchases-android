package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.AdRewardConfiguration
import com.revenuecat.purchases.ads.ConfiguredAdRewards
import com.revenuecat.purchases.interfaces.GetAdRewardsCallback
import kotlin.time.Duration

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("unused", "UNUSED_VARIABLE")
private class ConfiguredAdRewardsAPI {
    fun check(
        rewards: ConfiguredAdRewards,
        entitlement: AdRewardConfiguration.Entitlement,
        currency: AdRewardConfiguration.VirtualCurrency,
    ) {
        val primary: AdRewardConfiguration? = rewards.reward
        val additional: List<AdRewardConfiguration> = rewards.moreRewards
        val identifier: String = entitlement.identifier
        val duration: Duration = entitlement.duration
        val seconds: Long = entitlement.durationInSeconds
        val code: String = currency.code
        val amount: Int = currency.amount
        val unsupported: AdRewardConfiguration = AdRewardConfiguration.UnsupportedReward
        val noReward: Boolean = rewards.reward == null
    }

    fun checkCallback(rewards: ConfiguredAdRewards) {
        val callback = object : GetAdRewardsCallback {
            override fun onReceived(rewards: ConfiguredAdRewards?) {
                val unavailable: Boolean = rewards == null
            }
        }
        callback.onReceived(rewards)
        callback.onReceived(null)
        val samCallback = GetAdRewardsCallback { result: ConfiguredAdRewards? -> }
    }
}
