@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import kotlin.jvm.JvmSynthetic

/**
 * Shows this interstitial and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun InterstitialAd.show(activity: Activity, placement: String?) {
    val trackingCallback = adEventCallback as? TrackingInterstitialAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.placement = placement
    } else {
        Logger.w("Placement override ignored: adEventCallback was manually reassigned")
    }
    show(activity)
}

/**
 * Sets an [InterstitialAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [InterstitialAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackInterstitialAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun InterstitialAd.setTrackingAdEventCallback(callback: InterstitialAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingInterstitialAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}
