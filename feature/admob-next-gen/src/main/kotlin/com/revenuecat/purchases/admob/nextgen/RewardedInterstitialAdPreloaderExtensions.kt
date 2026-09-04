
package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingPreloadCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Starts rewarded-interstitial preloading and tracks every preload success or failure.
 *
 * The callback only represents preload completion. Call [pollAndTrackAd] later to adopt a buffered ad and install
 * tracking for its display, click, and revenue lifecycle without emitting another loaded event.
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param preloadConfiguration Google's preload configuration. Its request ad unit ID is used for tracking.
 * @param placement Optional placement attached to preload success and failure events.
 * @param preloadCallback Optional callback that receives every Google preload callback.
 * @return Google's unchanged result indicating whether preloading started.
 */
@JvmSynthetic
public fun RewardedInterstitialAdPreloader.Companion.startAndTrack(
    preloadId: String,
    preloadConfiguration: PreloadConfiguration,
    placement: String? = null,
    preloadCallback: PreloadCallback? = null,
): Boolean = start(
    preloadId,
    preloadConfiguration,
    TrackingPreloadCallback(
        delegate = preloadCallback,
        adFormat = AdFormat.REWARDED_INTERSTITIAL,
        placement = placement,
        adUnitId = preloadConfiguration.request.adUnitId,
    ),
)

/**
 * Polls a buffered rewarded-interstitial ad and installs RevenueCat lifecycle tracking before returning it.
 *
 * This does not track a loaded event because preload completion is reported by [startAndTrack]. If preloading was
 * started through Google's plain API, later lifecycle events can still be tracked, but the original load cannot be
 * reported retroactively.
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param placement Optional placement for lifecycle events, independent from the start-time placement.
 * @param adEventCallback Optional callback for rewarded-interstitial lifecycle, metadata, and paid events.
 */
@JvmSynthetic
public fun RewardedInterstitialAdPreloader.Companion.pollAndTrackAd(
    preloadId: String,
    placement: String? = null,
    adEventCallback: RewardedInterstitialAdEventCallback? = null,
): RewardedInterstitialAd? {
    val ad = pollAd(preloadId) ?: return null
    ad.installPreloaderTrackingCallbacks(adEventCallback, placement)
    return ad
}

private fun RewardedInterstitialAd.installPreloaderTrackingCallbacks(
    adEventCallback: RewardedInterstitialAdEventCallback?,
    placement: String?,
) {
    this.adEventCallback = TrackingRewardedInterstitialAdEventCallback(
        initialDelegate = adEventCallback,
        initialPlacement = placement,
        adUnitId = adUnitId,
        responseInfoProvider = ::getResponseInfo,
    )
}
