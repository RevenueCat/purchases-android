@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlin.jvm.JvmSynthetic

private const val TAG = "PurchasesAdMob"

private fun applyPlacementOverride(callback: FullScreenContentCallback?, placement: String?) {
    val trackingCallback = callback as? TrackingFullScreenContentCallback
    if (trackingCallback != null) {
        trackingCallback.placement = placement
    } else {
        Log.w(
            TAG,
            "Placement override ignored: fullScreenContentCallback was manually reassigned",
        )
    }
}

/**
 * Shows the interstitial ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [InterstitialAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun InterstitialAd.show(activity: Activity, placement: String?) {
    applyPlacementOverride(fullScreenContentCallback, placement)
    show(activity)
}

/**
 * Shows the app open ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [AppOpenAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AppOpenAd.show(activity: Activity, placement: String?) {
    applyPlacementOverride(fullScreenContentCallback, placement)
    show(activity)
}

/**
 * Shows the rewarded ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [RewardedAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    applyPlacementOverride(fullScreenContentCallback, placement)
    show(activity, onUserEarnedRewardListener)
}

/**
 * Shows the rewarded interstitial ad and overrides the placement used for RevenueCat analytics.
 *
 * Call this instead of [RewardedInterstitialAd.show] when you want to specify or override the placement
 * at show time. The placement passed here takes precedence over any placement provided at load time.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    applyPlacementOverride(fullScreenContentCallback, placement)
    show(activity, onUserEarnedRewardListener)
}

// region Safe callback reassignment

/**
 * Safely sets your [FullScreenContentCallback] without removing RevenueCat's tracking wrapper.
 *
 * Use this instead of assigning [InterstitialAd.fullScreenContentCallback] directly when the ad
 * was loaded via [AdTracker.loadAndTrackInterstitialAd]. If the ad was not loaded via
 * `loadAndTrack`, this falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun InterstitialAd.setTrackingFullScreenContentCallback(callback: FullScreenContentCallback?) {
    val wrapper = fullScreenContentCallback as? TrackingFullScreenContentCallback
    if (wrapper != null) {
        wrapper.delegate = callback
    } else {
        fullScreenContentCallback = callback
    }
}

/**
 * Safely sets your [FullScreenContentCallback] without removing RevenueCat's tracking wrapper.
 *
 * Use this instead of assigning [AppOpenAd.fullScreenContentCallback] directly when the ad
 * was loaded via [AdTracker.loadAndTrackAppOpenAd]. If the ad was not loaded via
 * `loadAndTrack`, this falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun AppOpenAd.setTrackingFullScreenContentCallback(callback: FullScreenContentCallback?) {
    val wrapper = fullScreenContentCallback as? TrackingFullScreenContentCallback
    if (wrapper != null) {
        wrapper.delegate = callback
    } else {
        fullScreenContentCallback = callback
    }
}

/**
 * Safely sets your [FullScreenContentCallback] without removing RevenueCat's tracking wrapper.
 *
 * Use this instead of assigning [RewardedAd.fullScreenContentCallback] directly when the ad
 * was loaded via [AdTracker.loadAndTrackRewardedAd]. If the ad was not loaded via
 * `loadAndTrack`, this falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun RewardedAd.setTrackingFullScreenContentCallback(callback: FullScreenContentCallback?) {
    val wrapper = fullScreenContentCallback as? TrackingFullScreenContentCallback
    if (wrapper != null) {
        wrapper.delegate = callback
    } else {
        fullScreenContentCallback = callback
    }
}

/**
 * Safely sets your [FullScreenContentCallback] without removing RevenueCat's tracking wrapper.
 *
 * Use this instead of assigning [RewardedInterstitialAd.fullScreenContentCallback] directly when
 * the ad was loaded via [AdTracker.loadAndTrackRewardedInterstitialAd]. If the ad was not loaded
 * via `loadAndTrack`, this falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
public fun RewardedInterstitialAd.setTrackingFullScreenContentCallback(callback: FullScreenContentCallback?) {
    val wrapper = fullScreenContentCallback as? TrackingFullScreenContentCallback
    if (wrapper != null) {
        wrapper.delegate = callback
    } else {
        fullScreenContentCallback = callback
    }
}

// endregion
