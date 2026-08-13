@file:JvmName("RCAdMobNextGenRewardedInterstitialAd")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import kotlin.jvm.JvmSynthetic

/**
 * Shows this rewarded interstitial and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    val trackingCallback = adEventCallback as? TrackingRewardedInterstitialAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.placement = placement
    } else {
        Logger.w("Placement override ignored: adEventCallback was manually reassigned")
    }
    show(activity, onUserEarnedRewardListener)
}

/**
 * Sets a [RewardedInterstitialAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [RewardedInterstitialAd.adEventCallback] directly when the ad was
 * loaded via [loadAndTrackRewardedInterstitialAd]. If the ad was not loaded via `loadAndTrack`,
 * this falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.setTrackingAdEventCallback(callback: RewardedInterstitialAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingRewardedInterstitialAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}
