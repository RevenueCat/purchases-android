@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import kotlin.jvm.JvmSynthetic

/**
 * Sets a [BannerAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [BannerAd.adEventCallback] directly when the ad was loaded
 * via [AdView.loadAndTrackAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun BannerAd.setTrackingAdEventCallback(callback: BannerAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingBannerAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

/**
 * Sets a [BannerAdRefreshCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [BannerAd.bannerAdRefreshCallback] directly when the ad was
 * loaded via [AdView.loadAndTrackAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun BannerAd.setTrackingBannerAdRefreshCallback(callback: BannerAdRefreshCallback?) {
    val trackingCallback = bannerAdRefreshCallback as? TrackingBannerAdRefreshCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        bannerAdRefreshCallback = callback
    }
}
