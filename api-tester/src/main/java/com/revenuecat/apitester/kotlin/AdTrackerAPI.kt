package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRewardEarnedUnverifiedData

@Suppress("unused", "UNUSED_VARIABLE")
private class AdTrackerAPI {
    fun check(adTracker: AdTracker) {
        val rewardEarned = AdRewardEarnedUnverifiedData(
            networkName = "AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "home_screen",
            adUnitId = "ca-app-pub-123",
            impressionId = "impression-456",
        )

        val networkName: String? = rewardEarned.networkName
        val mediatorName: AdMediatorName = rewardEarned.mediatorName
        val adFormat: AdFormat = rewardEarned.adFormat
        val placement: String? = rewardEarned.placement
        val adUnitId: String = rewardEarned.adUnitId
        val impressionId: String = rewardEarned.impressionId
        val rewardVerificationEnabled: Boolean = rewardEarned.rewardVerificationEnabled

        adTracker.trackAdRewardEarnedUnverified(rewardEarned)
    }
}
