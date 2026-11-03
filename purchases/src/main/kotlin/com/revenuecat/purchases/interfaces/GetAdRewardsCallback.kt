package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.ConfiguredAdRewards

/** Receives configured rewards, or null when configuration is unavailable or the ad unit ID is empty. */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun interface GetAdRewardsCallback {
    /** Called on the main thread with the lookup result. */
    public fun onReceived(rewards: ConfiguredAdRewards?)
}
