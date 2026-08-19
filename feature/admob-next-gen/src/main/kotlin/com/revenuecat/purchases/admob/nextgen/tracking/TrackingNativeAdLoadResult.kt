@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat

/** Tracks one result emitted by Google Mobile Ads' multiple-native-ad Flow API. */
internal fun trackNativeAdLoadResult(
    result: NativeAdLoadResult,
    placement: String?,
    adUnitId: String,
) {
    when (result) {
        is NativeAdLoadResult.NativeAdSuccess -> trackAdLoaded(
            responseInfo = result.ad.getResponseInfo(),
            adFormat = AdFormat.NATIVE,
            placement = placement,
            adUnitId = adUnitId,
        )
        is NativeAdLoadResult.CustomNativeAdSuccess -> trackAdLoaded(
            responseInfo = result.ad.getResponseInfo(),
            adFormat = AdFormat.NATIVE,
            placement = placement,
            adUnitId = adUnitId,
        )
        is NativeAdLoadResult.BannerAdSuccess -> trackAdLoaded(
            responseInfo = result.ad.getResponseInfo(),
            adFormat = AdFormat.BANNER,
            placement = placement,
            adUnitId = adUnitId,
        )
        is NativeAdLoadResult.Failure -> trackAdFailedToLoad(
            adError = result.error,
            adFormat = AdFormat.NATIVE,
            placement = placement,
            adUnitId = adUnitId,
        )
    }
}
