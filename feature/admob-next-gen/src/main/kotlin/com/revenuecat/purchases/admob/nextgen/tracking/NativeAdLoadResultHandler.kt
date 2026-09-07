package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.revenuecat.purchases.ads.events.types.AdFormat

internal class NativeAdLoadResultHandler(
    private val placement: String?,
    private val adUnitId: String,
    private val configureAd: (NativeAd) -> Unit,
    private val configureCustomNativeAd: (CustomNativeAd) -> Unit = {},
    private val configureBannerAd: (BannerAd) -> Unit = {},
) {
    fun handle(result: NativeAdLoadResult) {
        when (result) {
            is NativeAdLoadResult.NativeAdSuccess -> {
                trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
                configureAd(result.ad)
            }
            is NativeAdLoadResult.CustomNativeAdSuccess -> {
                trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.NATIVE, placement, adUnitId)
                configureCustomNativeAd(result.ad)
            }
            is NativeAdLoadResult.BannerAdSuccess -> {
                trackAdLoaded({ result.ad.getResponseInfo() }, AdFormat.BANNER, placement, adUnitId)
                configureBannerAd(result.ad)
            }
            is NativeAdLoadResult.Failure -> {
                trackAdFailedToLoad(result.error, AdFormat.NATIVE, placement, adUnitId)
            }
        }
    }
}
