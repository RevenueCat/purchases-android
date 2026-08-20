@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.applyPlacementOverride
import kotlin.jvm.JvmSynthetic

/**
 * Shows this rewarded interstitial and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 * Passing `null` clears the load-time placement instead of keeping it, so call the Next-Gen SDK's
 * own `show(activity, onUserEarnedRewardListener)` when there is no override to apply.
 * The [onUserEarnedRewardListener] is forwarded unchanged to Google Mobile Ads.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    adEventCallback.applyPlacementOverride(placement)
    show(activity, onUserEarnedRewardListener)
}

/**
 * Sets a [RewardedInterstitialAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [RewardedInterstitialAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackRewardedInterstitialAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
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

internal fun RewardedInterstitialAd.installTrackingEventCallback(
    delegate: RewardedInterstitialAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingRewardedInterstitialAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
