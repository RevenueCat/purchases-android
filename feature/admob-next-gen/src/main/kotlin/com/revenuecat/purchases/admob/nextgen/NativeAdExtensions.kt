@file:JvmName("RCAdMobNextGenNativeAd")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
import kotlin.jvm.JvmSynthetic

/**
 * Sets a [NativeAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [NativeAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackNativeAd]. If the ad was not loaded via `loadAndTrack`, this falls back to
 * direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun NativeAd.setTrackingAdEventCallback(callback: NativeAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingNativeAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}
