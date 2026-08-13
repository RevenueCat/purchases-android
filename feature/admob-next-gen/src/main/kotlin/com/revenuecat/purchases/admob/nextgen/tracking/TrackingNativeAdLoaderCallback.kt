@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat

/**
 * Tracks standard native load results while preserving every callback that the loader can emit.
 * Custom native and banner results are delegated unchanged because they are separate ad formats.
 */
internal class TrackingNativeAdLoaderCallback(
    private val delegate: NativeAdLoaderCallback?,
    private val placement: String?,
    private val adUnitId: String,
    private val adEventCallback: NativeAdEventCallback?,
) : NativeAdLoaderCallback {

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        trackAdLoaded(nativeAd.getResponseInfo(), AdFormat.NATIVE, placement, adUnitId)
        nativeAd.adEventCallback = TrackingNativeAdEventCallback(
            delegate = adEventCallback,
            placement = placement,
            adUnitId = adUnitId,
            responseInfoProvider = nativeAd::getResponseInfo,
        )
        delegate?.onNativeAdLoaded(nativeAd)
    }

    override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
        delegate?.onCustomNativeAdLoaded(customNativeAd)
    }

    override fun onBannerAdLoaded(bannerAd: BannerAd) {
        delegate?.onBannerAdLoaded(bannerAd)
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        trackAdFailedToLoad(adError, AdFormat.NATIVE, placement, adUnitId)
        delegate?.onAdFailedToLoad(adError)
    }

    override fun onAdLoadingCompleted() {
        delegate?.onAdLoadingCompleted()
    }
}
