@file:JvmName("RCAdMobNextGenAppOpenAd")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
import kotlin.jvm.JvmSynthetic

/**
 * Shows this app open ad and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AppOpenAd.show(activity: Activity, placement: String?) {
    val trackingCallback = adEventCallback as? TrackingAppOpenAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.placement = placement
    } else {
        Logger.w("Placement override ignored: adEventCallback was manually reassigned")
    }
    show(activity)
}

/**
 * Sets an [AppOpenAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [AppOpenAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackAppOpenAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AppOpenAd.setTrackingAdEventCallback(callback: AppOpenAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingAppOpenAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

internal fun AppOpenAd.installTrackingEventCallback(
    delegate: AppOpenAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingAppOpenAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
