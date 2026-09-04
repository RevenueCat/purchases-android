package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import io.mockk.every
import io.mockk.mockk

internal fun nativeAdRequest(adUnitId: String): NativeAdRequest = mockk {
    every { this@mockk.adUnitId } returns adUnitId
}

internal fun nativeAd(network: String, responseId: String): NativeAd {
    var callback: NativeAdEventCallback? = null
    return mockk(relaxed = true) {
        every { getResponseInfo() } returns responseInfo(network, responseId)
        every { adEventCallback } answers { callback }
        every { adEventCallback = any() } answers { callback = firstArg() }
    }
}

internal fun customNativeAd(network: String, responseId: String): CustomNativeAd {
    var callback: NativeAdEventCallback? = null
    return mockk(relaxed = true) {
        every { getResponseInfo() } returns responseInfo(network, responseId)
        every { adEventCallback } answers { callback }
        every { adEventCallback = any() } answers { callback = firstArg() }
    }
}

internal fun nativeBannerAd(network: String, responseId: String): BannerAd {
    var callback: BannerAdEventCallback? = null
    var refreshCallback: BannerAdRefreshCallback? = null
    return mockk(relaxed = true) {
        every { getResponseInfo() } returns responseInfo(network, responseId)
        every { adEventCallback } answers { callback }
        every { adEventCallback = any() } answers { callback = firstArg() }
        every { bannerAdRefreshCallback } answers { refreshCallback }
        every { bannerAdRefreshCallback = any() } answers { refreshCallback = firstArg() }
    }
}

internal class RecordingNativeAdLoaderCallback : NativeAdLoaderCallback {
    val nativeAds = mutableListOf<NativeAd>()
    val customNativeAds = mutableListOf<CustomNativeAd>()
    val bannerAds = mutableListOf<BannerAd>()
    val loadErrors = mutableListOf<LoadAdError>()
    var completionCount = 0
        private set

    override fun onNativeAdLoaded(nativeAd: NativeAd) {
        nativeAds += nativeAd
    }

    override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
        customNativeAds += customNativeAd
    }

    override fun onBannerAdLoaded(bannerAd: BannerAd) {
        bannerAds += bannerAd
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        loadErrors += adError
    }

    override fun onAdLoadingCompleted() {
        completionCount++
    }
}
