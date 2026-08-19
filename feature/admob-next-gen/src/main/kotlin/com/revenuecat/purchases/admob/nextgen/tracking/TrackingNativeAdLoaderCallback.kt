@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat

/**
 * A [NativeAdLoaderCallback] wrapper that tracks standard native load results and delegates every callback.
 *
 * Each configure callback runs after its ad is tracked and before it is forwarded to [delegate], allowing callers to
 * install post-load tracking without coupling this load wrapper to the event-tracking implementation. Custom native
 * results are tracked as native ads, while banner results are tracked as banner ads.
 */
internal class TrackingNativeAdLoaderCallback(
    private val delegate: NativeAdLoaderCallback?,
    private val placement: String?,
    private val adUnitId: String,
    private val configureAd: (NativeAd) -> Unit,
    private val configureCustomNativeAd: (CustomNativeAd) -> Unit = {},
    private val configureBannerAd: (BannerAd) -> Unit = {},
) : NativeAdLoaderCallback {

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        trackAdLoaded({ nativeAd.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
        configureAd(nativeAd)
        delegate?.onNativeAdLoaded(nativeAd)
    }

    override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
        trackAdLoaded({ customNativeAd.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
        configureCustomNativeAd(customNativeAd)
        delegate?.onCustomNativeAdLoaded(customNativeAd)
    }

    override fun onBannerAdLoaded(bannerAd: BannerAd) {
        trackAdLoaded({ bannerAd.getResponseInfo() }, AdFormat.BANNER, placement, adUnitId)
        configureBannerAd(bannerAd)
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
