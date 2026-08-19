@file:JvmName("RCAdMobNextGenNativeAds")
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdLoaderCallback
import com.revenuecat.purchases.admob.nextgen.tracking.trackNativeAdLoadResult
import com.revenuecat.purchases.ads.events.AdTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.jvm.JvmSynthetic

/**
 * Loads a batch of up to [maxNumberOfAds] native ads and automatically tracks every load result.
 *
 * The callback is invoked once for each result and [NativeAdLoaderCallback.onAdLoadingCompleted]
 * is forwarded after Google finishes the batch. A request can return standard native, custom-native,
 * or banner ads, and each successful result is tracked using its actual format.
 *
 * @param adRequest The native ad request. Its ad unit ID is used for tracking.
 * @param maxNumberOfAds Maximum number of ads to load.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 * @param nativeAdLoaderCallback Callback that receives every load result and batch completion.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun AdTracker.loadAndTrackNativeAds(
    adRequest: NativeAdRequest,
    maxNumberOfAds: Int,
    placement: String? = null,
    nativeAdLoaderCallback: NativeAdLoaderCallback,
) {
    NativeAdLoader.load(
        adRequest,
        maxNumberOfAds,
        TrackingNativeAdLoaderCallback(
            delegate = nativeAdLoaderCallback,
            placement = placement,
            adUnitId = adRequest.adUnitId,
            configureAd = {},
        ),
    )
}

/**
 * Loads up to [maxNumberOfAds] native ads and automatically tracks every result emitted by the returned [Flow].
 *
 * The original Google Mobile Ads results are returned unchanged. A request can emit standard native,
 * custom-native, banner, and failure results, and each result is tracked when it is collected.
 *
 * @param adRequest The native ad request. Its ad unit ID is used for tracking.
 * @param maxNumberOfAds Maximum number of ads to load.
 * @param placement Optional placement identifier used in RevenueCat tracking.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public suspend fun AdTracker.loadAndTrackNativeAds(
    adRequest: NativeAdRequest,
    maxNumberOfAds: Int,
    placement: String? = null,
): Flow<NativeAdLoadResult> =
    NativeAdLoader.load(adRequest, maxNumberOfAds).onEach { result ->
        trackNativeAdLoadResult(
            result = result,
            placement = placement,
            adUnitId = adRequest.adUnitId,
        )
    }
