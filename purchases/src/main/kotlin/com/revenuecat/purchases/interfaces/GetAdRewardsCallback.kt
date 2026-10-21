package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ads.ConfiguredAdRewards

/** Receives configured rewards or an error when configuration cannot be resolved. */
@ExperimentalPreviewRevenueCatPurchasesAPI
public interface GetAdRewardsCallback {
    /** Called on the main thread after configuration resolution completes. */
    public fun onReceived(rewards: ConfiguredAdRewards)

    /** Called on the main thread when configuration is unavailable or its JSON/container structure is invalid. */
    public fun onError(error: PurchasesError)
}
