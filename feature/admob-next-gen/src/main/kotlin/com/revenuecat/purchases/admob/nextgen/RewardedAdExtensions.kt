
package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.applyPlacementOverride
import kotlin.jvm.JvmSynthetic

/**
 * Shows this rewarded ad and overrides the placement used for RevenueCat analytics.
 *
 * The placement passed here takes precedence over the placement provided when the ad was loaded.
 * Passing `null` clears the load-time placement instead of keeping it, so call the Next-Gen SDK's
 * own `show(activity, onUserEarnedRewardListener)` when there is no override to apply.
 */
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String?,
    onUserEarnedRewardListener: OnUserEarnedRewardListener,
) {
    adEventCallback.applyPlacementOverride(placement)
    show(activity, onUserEarnedRewardListener)
}

/**
 * Sets a [RewardedAdEventCallback] without removing RevenueCat's tracking callback.
 *
 * Use this instead of assigning [RewardedAd.adEventCallback] directly when the ad was loaded
 * via [loadAndTrackRewardedAd]. If the ad was not loaded via `loadAndTrack`, this
 * falls back to direct assignment.
 */
@JvmSynthetic
public fun RewardedAd.setTrackingAdEventCallback(callback: RewardedAdEventCallback?) {
    val trackingCallback = adEventCallback as? TrackingRewardedAdEventCallback
    if (trackingCallback != null) {
        trackingCallback.delegate = callback
    } else {
        adEventCallback = callback
    }
}

internal fun RewardedAd.installTrackingEventCallback(
    delegate: RewardedAdEventCallback?,
    placement: String?,
    adUnitId: String,
) {
    adEventCallback = TrackingRewardedAdEventCallback(
        initialDelegate = delegate,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
