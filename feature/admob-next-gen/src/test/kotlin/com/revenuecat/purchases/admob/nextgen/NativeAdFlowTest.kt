@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class NativeAdFlowTest {
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
    fun `callback load tracks and configures every possible success before forwarding`() {
        val adRequest = adRequest("native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        val nativeEventCallback = mockk<NativeAdEventCallback>(relaxed = true)
        val bannerEventCallback = mockk<BannerAdEventCallback>(relaxed = true)
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()
        val delegate = RecordingNativeAdLoaderCallback()

        every { NativeAdLoader.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackNativeAd(
            adRequest = adRequest,
            placement = "feed",
            nativeAdLoaderCallback = delegate,
            nativeAdEventCallback = nativeEventCallback,
            bannerAdEventCallback = bannerEventCallback,
        )
        trackingLoadCallback.captured.onNativeAdLoaded(nativeAd)
        trackingLoadCallback.captured.onCustomNativeAdLoaded(customNativeAd)
        trackingLoadCallback.captured.onBannerAdLoaded(bannerAd)

        assertEquals(listOf(nativeAd), delegate.nativeAds)
        assertEquals(listOf(customNativeAd), delegate.customNativeAds)
        assertEquals(listOf(bannerAd), delegate.bannerAds)
        assertTrue(nativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(customNativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(bannerAd.adEventCallback is TrackingBannerAdEventCallback)

        val trackedLoads = mutableListOf<AdLoadedData>()
        verify(exactly = 3) {
            adTracker.trackAdLoaded(capture(trackedLoads), AdCaptureMethod.ADAPTER)
        }
        assertEquals(listOf(AdFormat.NATIVE, AdFormat.NATIVE, AdFormat.BANNER), trackedLoads.map { it.adFormat })
        assertEquals(listOf("feed", "feed", "feed"), trackedLoads.map { it.placement })
        assertEquals(listOf("native-unit", "native-unit", "native-unit"), trackedLoads.map { it.adUnitId })
    }

    @Test
    fun `callback load tracks failure before forwarding`() {
        val adRequest = adRequest("native-unit")
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NO_FILL
        }
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()
        val delegate = RecordingNativeAdLoaderCallback()

        every { NativeAdLoader.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackNativeAd(adRequest, "feed", delegate)
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, delegate.loadErrors.single())
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.NATIVE, failedData.captured.adFormat)
        assertEquals("feed", failedData.captured.placement)
        assertEquals("native-unit", failedData.captured.adUnitId)
    }

    @Test
    fun `suspending load returns and configures native result unchanged`() = runBlocking {
        val adRequest = adRequest("native-unit")
        val nativeAd = nativeAd("native-network", "native-response")
        val sdkResult = NativeAdLoadResult.NativeAdSuccess(nativeAd)

        coEvery { NativeAdLoader.load(adRequest) } returns sdkResult

        val result = adTracker.loadAndTrackNativeAd(adRequest, placement = "feed")

        assertSame(sdkResult, result)
        assertTrue(nativeAd.adEventCallback is TrackingNativeAdEventCallback)
        verify(exactly = 1) { adTracker.trackAdLoaded(any(), AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `suspending load maps custom native banner and failure results`() = runBlocking {
        val adRequest = adRequest("native-unit")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val results = listOf(
            NativeAdLoadResult.CustomNativeAdSuccess(customNativeAd),
            NativeAdLoadResult.BannerAdSuccess(bannerAd),
            NativeAdLoadResult.Failure(error),
        )
        coEvery { NativeAdLoader.load(adRequest) } returns results[0] andThen results[1] andThen results[2]

        val returnedResults = results.map {
            adTracker.loadAndTrackNativeAd(adRequest, placement = "feed")
        }

        results.zip(returnedResults).forEach { (expected, actual) -> assertSame(expected, actual) }
        assertTrue(customNativeAd.adEventCallback is TrackingNativeAdEventCallback)
        assertTrue(bannerAd.adEventCallback is TrackingBannerAdEventCallback)
        val trackedLoads = mutableListOf<AdLoadedData>()
        verify(exactly = 2) { adTracker.trackAdLoaded(capture(trackedLoads), AdCaptureMethod.ADAPTER) }
        assertEquals(listOf(AdFormat.NATIVE, AdFormat.BANNER), trackedLoads.map { it.adFormat })
        verify(exactly = 1) { adTracker.trackAdFailedToLoad(any(), AdCaptureMethod.ADAPTER) }
    }

    @Test
    fun `tracking-safe setters preserve native custom native and banner wrappers`() {
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        nativeAd.installTrackingEventCallback(null, "feed", "native-unit")
        customNativeAd.installTrackingEventCallback(null, "feed", "native-unit")
        bannerAd.installTrackingEventCallback(null, "feed", "native-unit")
        val nativeTrackingCallback = nativeAd.adEventCallback
        val customTrackingCallback = customNativeAd.adEventCallback
        val bannerTrackingCallback = bannerAd.adEventCallback

        nativeAd.setTrackingAdEventCallback(mockk(relaxed = true))
        customNativeAd.setTrackingAdEventCallback(mockk(relaxed = true))
        bannerAd.setTrackingAdEventCallback(mockk(relaxed = true))

        assertSame(nativeTrackingCallback, nativeAd.adEventCallback)
        assertSame(customTrackingCallback, customNativeAd.adEventCallback)
        assertSame(bannerTrackingCallback, bannerAd.adEventCallback)
    }

    @Test
    fun `tracking-safe setters directly assign callbacks when tracking is not installed`() {
        val nativeAd = nativeAd("native-network", "native-response")
        val customNativeAd = customNativeAd("custom-network", "custom-response")
        val bannerAd = bannerAd("banner-network", "banner-response")
        nativeAd.adEventCallback = mockk(relaxed = true)
        customNativeAd.adEventCallback = mockk(relaxed = true)
        bannerAd.adEventCallback = mockk(relaxed = true)
        val nativeCallback = mockk<NativeAdEventCallback>(relaxed = true)
        val customNativeCallback = mockk<NativeAdEventCallback>(relaxed = true)
        val bannerCallback = mockk<BannerAdEventCallback>(relaxed = true)

        nativeAd.setTrackingAdEventCallback(nativeCallback)
        customNativeAd.setTrackingAdEventCallback(customNativeCallback)
        bannerAd.setTrackingAdEventCallback(bannerCallback)

        assertSame(nativeCallback, nativeAd.adEventCallback)
        assertSame(customNativeCallback, customNativeAd.adEventCallback)
        assertSame(bannerCallback, bannerAd.adEventCallback)
    }

    private fun adRequest(adUnitId: String): NativeAdRequest = mockk {
        every { this@mockk.adUnitId } returns adUnitId
    }

    private fun nativeAd(network: String, responseId: String): NativeAd {
        var callback: NativeAdEventCallback? = null
        return mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo(network, responseId)
            every { adEventCallback } answers { callback }
            every { adEventCallback = any() } answers { callback = firstArg() }
        }
    }

    private fun customNativeAd(network: String, responseId: String): CustomNativeAd {
        var callback: NativeAdEventCallback? = null
        return mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo(network, responseId)
            every { adEventCallback } answers { callback }
            every { adEventCallback = any() } answers { callback = firstArg() }
        }
    }

    private fun bannerAd(network: String, responseId: String): BannerAd {
        var callback: BannerAdEventCallback? = null
        return mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo(network, responseId)
            every { adEventCallback } answers { callback }
            every { adEventCallback = any() } answers { callback = firstArg() }
        }
    }

    private fun responseInfo(network: String, responseId: String): ResponseInfo = mockk {
        every { adapterClassName } returns network
        every { this@mockk.responseId } returns responseId
    }

    private class RecordingNativeAdLoaderCallback : NativeAdLoaderCallback {
        val nativeAds = mutableListOf<NativeAd>()
        val customNativeAds = mutableListOf<CustomNativeAd>()
        val bannerAds = mutableListOf<BannerAd>()
        val loadErrors = mutableListOf<LoadAdError>()

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
    }
}
