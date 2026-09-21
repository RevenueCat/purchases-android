
package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
import kotlin.jvm.JvmSynthetic

/**
 * Sets a [NativeAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [NativeAd.adEventCallback] directly when the ad was loaded through a RevenueCat
 * native-ad tracking API. If tracking is not installed, this falls back to direct assignment.
 */
@JvmSynthetic
public fun NativeAd.setTrackingAdEventCallback(callback: NativeAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingNativeAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

/**
 * Sets a [NativeAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * This overload handles custom-native results returned by a RevenueCat native-ad tracking API. If tracking is not
 * installed, it falls back to direct assignment.
 */
@JvmSynthetic
public fun CustomNativeAd.setTrackingAdEventCallback(callback: NativeAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingNativeAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

internal fun NativeAd.installTrackingEventCallback(
    delegate: NativeAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingNativeAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}

internal fun CustomNativeAd.installTrackingEventCallback(
    delegate: NativeAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingNativeAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
