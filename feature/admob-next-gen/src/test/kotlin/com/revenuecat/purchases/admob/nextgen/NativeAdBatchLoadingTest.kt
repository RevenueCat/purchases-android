@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class NativeAdBatchLoadingTest {
    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(NativeAdLoader.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(NativeAdLoader.Companion)
    }

    @Test
    fun `callback batch tracks every loaded ad and forwards completion`() {
        val adRequest = adRequest("native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        val trackingCallback = slot<NativeAdLoaderCallback>()
        val delegate = RecordingNativeAdLoaderCallback()

        every { NativeAdLoader.load(adRequest, 3, capture(trackingCallback)) } just runs

        adTracker.loadAndTrackNativeAds(
            adRequest = adRequest,
            maxNumberOfAds = 3,
            placement = "feed",
            nativeAdLoaderCallback = delegate,
        )
        trackingCallback.captured.onNativeAdLoaded(nativeAd)
        trackingCallback.captured.onCustomNativeAdLoaded(customNativeAd)
        trackingCallback.captured.onBannerAdLoaded(bannerAd)
        trackingCallback.captured.onAdLoadingCompleted()

        val trackedLoads = mutableListOf<AdLoadedData>()
        verify(exactly = 3) {
            adTracker.trackAdLoaded(capture(trackedLoads), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            listOf(AdFormat.NATIVE, AdFormat.NATIVE, AdFormat.BANNER),
            trackedLoads.map { it.adFormat },
        )
        assertEquals(listOf("feed", "feed", "feed"), trackedLoads.map { it.placement })
        assertEquals(listOf("native-unit", "native-unit", "native-unit"), trackedLoads.map { it.adUnitId })
        assertEquals(listOf(nativeAd), delegate.nativeAds)
        assertEquals(listOf(customNativeAd), delegate.customNativeAds)
        assertEquals(listOf(bannerAd), delegate.bannerAds)
        assertEquals(1, delegate.loadingCompletedCount)
    }

    @Test
    fun `flow batch tracks and returns every result unchanged`() = runBlocking {
        val adRequest = adRequest("flow-native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NO_FILL
        }
        val sdkResults = listOf(
            NativeAdLoadResult.NativeAdSuccess(nativeAd),
            NativeAdLoadResult.CustomNativeAdSuccess(customNativeAd),
            NativeAdLoadResult.BannerAdSuccess(bannerAd),
            NativeAdLoadResult.Failure(error),
        )

        coEvery { NativeAdLoader.load(adRequest, 4) } returns flowOf(*sdkResults.toTypedArray())

        val returnedResults = adTracker.loadAndTrackNativeAds(
            adRequest = adRequest,
            maxNumberOfAds = 4,
            placement = "flow-feed",
        ).toList()

        sdkResults.zip(returnedResults).forEach { (expected, actual) -> assertSame(expected, actual) }

        val trackedLoads = mutableListOf<AdLoadedData>()
        verify(exactly = 3) {
            adTracker.trackAdLoaded(capture(trackedLoads), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            listOf(AdFormat.NATIVE, AdFormat.NATIVE, AdFormat.BANNER),
            trackedLoads.map { it.adFormat },
        )
        val trackedFailures = mutableListOf<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(trackedFailures), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.NATIVE, trackedFailures.single().adFormat)
        assertEquals("flow-feed", trackedFailures.single().placement)
        assertEquals("flow-native-unit", trackedFailures.single().adUnitId)
        assertEquals(LoadAdError.ErrorCode.NO_FILL.value, trackedFailures.single().mediatorErrorCode)
    }

    private fun adRequest(adUnitId: String): NativeAdRequest = mockk {
        every { this@mockk.adUnitId } returns adUnitId
    }

    private fun nativeAd(network: String, responseId: String): NativeAd = mockk {
        every { getResponseInfo() } returns responseInfo(network, responseId)
    }

    private fun customNativeAd(network: String, responseId: String): CustomNativeAd = mockk {
        every { getResponseInfo() } returns responseInfo(network, responseId)
    }

    private fun bannerAd(network: String, responseId: String): BannerAd = mockk {
        every { getResponseInfo() } returns responseInfo(network, responseId)
    }

    private fun responseInfo(network: String, responseId: String): ResponseInfo = mockk {
        every { adapterClassName } returns network
        every { this@mockk.responseId } returns responseId
    }

    private class RecordingNativeAdLoaderCallback : NativeAdLoaderCallback {
        val nativeAds = mutableListOf<NativeAd>()
        val customNativeAds = mutableListOf<CustomNativeAd>()
        val bannerAds = mutableListOf<BannerAd>()
        var loadingCompletedCount = 0

        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            nativeAds += nativeAd
        }

        override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
            customNativeAds += customNativeAd
        }

        override fun onBannerAdLoaded(bannerAd: BannerAd) {
            bannerAds += bannerAd
        }

        override fun onAdLoadingCompleted() {
            loadingCompletedCount++
        }
    }
}
