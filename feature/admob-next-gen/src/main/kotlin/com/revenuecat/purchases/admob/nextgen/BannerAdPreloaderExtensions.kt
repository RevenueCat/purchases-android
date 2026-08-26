@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingPreloadCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Starts banner preloading and tracks every preload success or failure.
 *
 * The callback only represents preload completion. Call [pollAndTrackAd] later to adopt a buffered ad and install
 * tracking for its display, click, revenue, and refresh lifecycle without emitting another loaded event.
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param preloadConfiguration Google's preload configuration. Its request ad unit ID is used for tracking.
 * @param placement Optional placement attached to preload success and failure events.
 * @param preloadCallback Optional callback that receives every Google preload callback.
 * @return Google's unchanged result indicating whether preloading started.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun BannerAdPreloader.Companion.startAndTrack(
    preloadId: String,
    preloadConfiguration: PreloadConfiguration,
    placement: String? = null,
    preloadCallback: PreloadCallback? = null,
): Boolean = start(
    preloadId,
    preloadConfiguration,
    TrackingPreloadCallback(
        delegate = preloadCallback,
        adFormat = AdFormat.BANNER,
        placement = placement,
        adUnitId = preloadConfiguration.request.adUnitId,
    ),
)

/**
 * Polls a buffered banner and installs RevenueCat lifecycle tracking before returning it.
 *
 * This does not track a loaded event because preload completion is reported by [startAndTrack]. If preloading was
 * started through Google's plain API, later lifecycle events can still be tracked, but the original load cannot be
 * reported retroactively. Register a returned ad with Google's normal `AdView.registerBannerAd` API.
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param placement Optional placement for lifecycle and refresh events, independent from the start-time placement.
 * @param adEventCallback Optional callback for banner lifecycle and paid events.
 * @param bannerAdRefreshCallback Optional callback for automatic banner refresh events.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun BannerAdPreloader.Companion.pollAndTrackAd(
    preloadId: String,
    placement: String? = null,
    adEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
): BannerAd? {
    val ad = pollAd(preloadId) ?: return null
    ad.installTrackingCallbacks(
        adEventCallback = adEventCallback,
        bannerAdRefreshCallback = bannerAdRefreshCallback,
        placement = placement,
        adUnitId = ad.adUnitId,
    )
    return ad
}
