package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ads.ConfiguredAdRewards

/** Receives configured rewards, or null when configuration cannot be resolved or the ad unit ID is empty. */
internal fun interface GetAdRewardsCallback {
    /** Called on the main thread after configuration resolution completes. */
    public fun onReceived(rewards: ConfiguredAdRewards?)
}
