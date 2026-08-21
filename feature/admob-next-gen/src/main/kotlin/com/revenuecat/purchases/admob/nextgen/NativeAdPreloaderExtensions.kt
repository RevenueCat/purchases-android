@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingPreloadCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import kotlin.jvm.JvmSynthetic

/**
 * Starts native ad preloading and tracks every preload success or failure.
 *
 * The callback only represents preload completion. Call [pollAndTrackAd] later to adopt a buffered result and install
 * tracking on its native, custom-native, or banner ad without emitting another loaded event.
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param preloadConfiguration Google's preload configuration. Its request ad unit ID is used for tracking.
 * @param placement Optional placement attached to preload success and failure events.
 * @param preloadCallback Optional callback that receives every Google preload callback.
 * @return Google's unchanged result indicating whether preloading started.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun NativeAdPreloader.Companion.startAndTrack(
    preloadId: String,
    preloadConfiguration: PreloadConfiguration,
    placement: String? = null,
    preloadCallback: PreloadCallback? = null,
): Boolean = start(
    preloadId,
    preloadConfiguration,
    TrackingPreloadCallback(
        delegate = preloadCallback,
        adFormat = AdFormat.NATIVE,
        placement = placement,
        adUnitId = preloadConfiguration.request.adUnitId,
    ),
)

/**
 * Polls a buffered native load result and installs RevenueCat lifecycle tracking before returning it.
 *
 * Google's native preloader can return a native ad, custom-native ad, or banner ad. Tracking is installed on the
 * concrete result before it is returned. This does not track a loaded event because preload completion is reported
 * by [startAndTrack].
 *
 * @param preloadId Identifier for Google's preloader buffer.
 * @param placement Optional placement for lifecycle and banner-refresh events, independent from the start placement.
 * @param nativeAdEventCallback Optional callback used for native and custom-native lifecycle and paid events.
 * @param bannerAdEventCallback Optional callback used when Google returns a banner result.
 * @param bannerAdRefreshCallback Optional refresh callback used when Google returns a banner result.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun NativeAdPreloader.Companion.pollAndTrackAd(
    preloadId: String,
    placement: String? = null,
    nativeAdEventCallback: NativeAdEventCallback? = null,
    bannerAdEventCallback: BannerAdEventCallback? = null,
    bannerAdRefreshCallback: BannerAdRefreshCallback? = null,
): NativeAdLoadResult.NativeAdLoadSuccessResult? {
    val result = pollAd(preloadId) ?: return null
    result.installPreloaderTrackingCallbacks(
        nativeAdEventCallback = nativeAdEventCallback,
        bannerAdEventCallback = bannerAdEventCallback,
        bannerAdRefreshCallback = bannerAdRefreshCallback,
        placement = placement,
    )
    return result
}

private fun NativeAdLoadResult.NativeAdLoadSuccessResult.installPreloaderTrackingCallbacks(
    nativeAdEventCallback: NativeAdEventCallback?,
    bannerAdEventCallback: BannerAdEventCallback?,
    bannerAdRefreshCallback: BannerAdRefreshCallback?,
    placement: String?,
) {
    when (this) {
        is NativeAdLoadResult.NativeAdSuccess -> ad.installTrackingEventCallback(
            delegate = nativeAdEventCallback,
            placement = placement,
            adUnitId = ad.adUnitId,
        )

        is NativeAdLoadResult.CustomNativeAdSuccess -> ad.installTrackingEventCallback(
            delegate = nativeAdEventCallback,
            placement = placement,
            adUnitId = ad.adUnitId,
        )

        is NativeAdLoadResult.BannerAdSuccess -> ad.installTrackingCallbacks(
            adEventCallback = bannerAdEventCallback,
            bannerAdRefreshCallback = bannerAdRefreshCallback,
            placement = placement,
            adUnitId = ad.adUnitId,
        )
    }
}
