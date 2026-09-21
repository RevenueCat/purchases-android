
package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback

/**
 * A [NativeAdLoaderCallback] wrapper that tracks standard native load results and delegates every callback.
 *
 * [resultHandler] tracks and configures each result before it is forwarded to [delegate]. Custom native results are
 * tracked as native ads, while banner results are tracked as banner ads.
 */
internal class TrackingNativeAdLoaderCallback(
    private val delegate: NativeAdLoaderCallback?,
    private val resultHandler: NativeAdLoadResultHandler,
) : NativeAdLoaderCallback {

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        resultHandler.handle(NativeAdLoadResult.NativeAdSuccess(nativeAd))
        delegate?.onNativeAdLoaded(nativeAd)
    }

    override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
        resultHandler.handle(NativeAdLoadResult.CustomNativeAdSuccess(customNativeAd))
        delegate?.onCustomNativeAdLoaded(customNativeAd)
    }

    override fun onBannerAdLoaded(bannerAd: BannerAd) {
        resultHandler.handle(NativeAdLoadResult.BannerAdSuccess(bannerAd))
        delegate?.onBannerAdLoaded(bannerAd)
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        resultHandler.handle(NativeAdLoadResult.Failure(adError))
        delegate?.onAdFailedToLoad(adError)
    }

    override fun onAdLoadingCompleted() {
        delegate?.onAdLoadingCompleted()
    }
}
