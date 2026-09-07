@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NativeAdBatchFlowTest {
    @get:Rule
    val configuredPurchases = ConfiguredPurchasesRule()

    private val adTracker get() = configuredPurchases.adTracker

    @Before
    fun setUp() {
        mockkObject(NativeAdLoader.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(NativeAdLoader.Companion)
    }

    @Test
    fun `callback batch tracks configures and forwards every result and completion`() {
        val adRequest = nativeAdRequest("native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = nativeBannerAd("banner-network", "banner-response")
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NO_FILL
        }
        val trackingCallback = slot<NativeAdLoaderCallback>()
        val delegate = RecordingNativeAdLoaderCallback()

        every { NativeAdLoader.load(adRequest, 4, capture(trackingCallback)) } just runs

        adTracker.loadAndTrackNativeAds(adRequest, 4, "feed", delegate)
        trackingCallback.captured.onNativeAdLoaded(nativeAd)
        trackingCallback.captured.onCustomNativeAdLoaded(customNativeAd)
        trackingCallback.captured.onBannerAdLoaded(bannerAd)
        trackingCallback.captured.onAdFailedToLoad(error)
        trackingCallback.captured.onAdLoadingCompleted()

        assertEquals(listOf(nativeAd), delegate.nativeAds)
        assertEquals(listOf(customNativeAd), delegate.customNativeAds)
        assertEquals(listOf(bannerAd), delegate.bannerAds)
        assertEquals(listOf(error), delegate.loadErrors)
        assertEquals(1, delegate.completionCount)
        assertTrue(nativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(customNativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(bannerAd.adEventCallback is TrackingBannerAdEventCallback)
        verify(exactly = 3) { adTracker.trackAdLoaded(any(), AdCaptureMethod.ADAPTER) }
        verify(exactly = 1) { adTracker.trackAdFailedToLoad(any(), AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `flow batch tracks and configures each result when collected`() = runBlocking {
        val adRequest = nativeAdRequest("native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = nativeBannerAd("banner-network", "banner-response")
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

        val flow = adTracker.loadAndTrackNativeAds(adRequest, 4, placement = "feed")
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
        val returnedResults = flow.toList()

        sdkResults.zip(returnedResults).forEach { (expected, actual) -> assertSame(expected, actual) }
        assertTrue(nativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(customNativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(bannerAd.adEventCallback is TrackingBannerAdEventCallback)
        val trackedLoads = mutableListOf<AdLoadedData>()
        verify(exactly = 3) { adTracker.trackAdLoaded(capture(trackedLoads), AdCaptureMethod.ADAPTER) }
        assertEquals(listOf(AdFormat.NATIVE, AdFormat.NATIVE, AdFormat.BANNER), trackedLoads.map { it.adFormat })
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.NATIVE, failedData.captured.adFormat)
        assertEquals("feed", failedData.captured.placement)
        assertEquals("native-unit", failedData.captured.adUnitId)
    }

}
