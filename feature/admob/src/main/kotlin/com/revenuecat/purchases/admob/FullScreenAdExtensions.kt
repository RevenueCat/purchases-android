@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Shows the interstitial ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [InterstitialAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun InterstitialAd.show(activity: Activity, placement: String?) {
    (fullScreenContentCallback as? TrackingFullScreenContentCallback)?.placement = placement
    show(activity)
}

/**
 * Shows the app open ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [AppOpenAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun AppOpenAd.show(activity: Activity, placement: String?) {
    (fullScreenContentCallback as? TrackingFullScreenContentCallback)?.placement = placement
    show(activity)
}

/**
 * Shows the rewarded ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [RewardedAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun RewardedAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    (fullScreenContentCallback as? TrackingFullScreenContentCallback)?.placement = placement
    show(activity, onUserEarnedRewardListener)
}

/**
 * Shows the rewarded interstitial ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [RewardedInterstitialAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    (fullScreenContentCallback as? TrackingFullScreenContentCallback)?.placement = placement
    show(activity, onUserEarnedRewardListener)
}
