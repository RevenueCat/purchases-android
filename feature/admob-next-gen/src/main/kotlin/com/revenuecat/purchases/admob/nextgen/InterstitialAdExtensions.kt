
package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.applyPlacementOverride
import kotlin.jvm.JvmSynthetic

/**
 * Shows this interstitial and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 * Passing `null` clears the load-time placement instead of keeping it, so call the Next-Gen SDK's
 * own `show(activity)` when there is no override to apply.
 */
@JvmSynthetic
public fun InterstitialAd.show(activity: Activity, placement: String?) {
    adEventCallback.applyPlacementOverride(placement)
    show(activity)
}

/**
 * Sets an [InterstitialAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [InterstitialAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackInterstitialAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@JvmSynthetic
public fun InterstitialAd.setTrackingAdEventCallback(callback: InterstitialAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingInterstitialAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

internal fun InterstitialAd.installTrackingEventCallback(
    delegate: InterstitialAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingInterstitialAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
