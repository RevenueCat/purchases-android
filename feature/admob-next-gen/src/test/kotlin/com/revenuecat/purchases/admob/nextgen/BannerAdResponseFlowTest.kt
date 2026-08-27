@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
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
class BannerAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    @Test
    fun `callback response success tracks and installs callbacks before forwarding`() {
        val order = mutableListOf<String>()
        val adView = mockk<AdView>()
        val responseInfo = responseInfo()
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
        }
        val eventCallback = RecordingBannerEventCallback()
        val refreshCallback = RecordingBannerRefreshCallback()
        val trackingLoadCallback = slot<AdLoadCallback<BannerAd>>()
        val installedEventCallback = slot<BannerAdEventCallback>()
        val installedRefreshCallback = slot<BannerAdRefreshCallback>()
        val loadedData = slot<AdLoadedData>()
        val loadCallback = object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                order += "load-callback"
                assertSame(bannerAd, ad)
            }
        }

        every { adView.loadFromAdResponse("opaque-response", capture(trackingLoadCallback)) } just runs
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }
        every { bannerAd.adEventCallback = capture(installedEventCallback) } answers {
            order += "event-callback"
        }
        every { bannerAd.bannerAdRefreshCallback = capture(installedRefreshCallback) } answers {
            order += "refresh-callback"
        }

        adTracker.loadAndTrackBannerAdFromResponse(
            adView = adView,
            adResponse = "opaque-response",
            adUnitId = "supplied-banner-unit",
            placement = "response-banner",
            loadCallback = loadCallback,
            adEventCallback = eventCallback,
            bannerAdRefreshCallback = refreshCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(bannerAd)

        assertEquals(listOf("tracked", "event-callback", "refresh-callback", "load-callback"), order)
        assertLoadedData(loadedData.captured)
        assertTrue(installedEventCallback.captured is TrackingBannerAdEventCallback)
        assertTrue(installedRefreshCallback.captured is TrackingBannerAdRefreshCallback)
        installedEventCallback.captured.onAppEvent("name", "data")
        installedRefreshCallback.captured.onAdRefreshed()
        assertTrue(eventCallback.appEventCalled)
        assertTrue(refreshCallback.refreshedCalled)
    }

    @Test
    fun `callback response failure uses supplied attribution before forwarding`() {
        val order = mutableListOf<String>()
        val adView = mockk<AdView>()
        val error = mockk<LoadAdError>(relaxed = true)
        val trackingLoadCallback = slot<AdLoadCallback<BannerAd>>()
        val failedData = slot<AdFailedToLoadData>()
        val loadCallback = object : AdLoadCallback<BannerAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                order += "load-callback"
                assertSame(error, adError)
            }
        }

        every { adView.loadFromAdResponse("opaque-response", capture(trackingLoadCallback)) } just runs
        every { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        adView.loadAndTrackAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-banner-unit",
            placement = "response-banner",
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertEquals(listOf("tracked", "load-callback"), order)
        assertFailedData(failedData.captured)
    }

    @Test
    fun `suspending response success returns original result after tracking and callback installation`() =
        runBlocking {
            val adView = mockk<AdView>()
            val bannerAd = mockk<BannerAd>(relaxed = true) {
                every { getResponseInfo() } returns responseInfo()
            }
            val eventCallback = RecordingBannerEventCallback()
            val refreshCallback = RecordingBannerRefreshCallback()
            val installedEventCallback = slot<BannerAdEventCallback>()
            val installedRefreshCallback = slot<BannerAdRefreshCallback>()
            val sdkResult = AdLoadResult.Success(bannerAd)

            coEvery { adView.loadFromAdResponse("opaque-response") } returns sdkResult
            every { bannerAd.adEventCallback = capture(installedEventCallback) } just runs
            every { bannerAd.bannerAdRefreshCallback = capture(installedRefreshCallback) } just runs

            val result = adTracker.loadAndTrackBannerAdFromResponse(
                adView = adView,
                adResponse = "opaque-response",
                adUnitId = "supplied-banner-unit",
                placement = "response-banner",
                adEventCallback = eventCallback,
                bannerAdRefreshCallback = refreshCallback,
            )

            assertSame(sdkResult, result)
            val loadedData = slot<AdLoadedData>()
            verify(exactly = 1) {
                adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
            }
            assertLoadedData(loadedData.captured)
            assertTrue(installedEventCallback.captured is TrackingBannerAdEventCallback)
            assertTrue(installedRefreshCallback.captured is TrackingBannerAdRefreshCallback)
        }

    @Test
    fun `suspending response failure returns original result after tracking supplied attribution`() = runBlocking {
        val adView = mockk<AdView>()
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.INVALID_AD_RESPONSE
        }
        val sdkResult = AdLoadResult.Failure<BannerAd>(error)

        coEvery { adView.loadFromAdResponse("opaque-response") } returns sdkResult

        val result = adView.loadAndTrackAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-banner-unit",
            placement = "response-banner",
        )

        assertSame(sdkResult, result)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertFailedData(failedData.captured)
    }

    private fun responseInfo(): ResponseInfo = mockk(relaxed = true) {
        every { adapterClassName } returns "test-network"
        every { responseId } returns "response-id"
    }

    private fun assertLoadedData(data: AdLoadedData) {
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "response-banner",
                adUnitId = "supplied-banner-unit",
                impressionId = "response-id",
            ),
            data,
        )
    }

    private fun assertFailedData(data: AdFailedToLoadData) {
        assertEquals(AdFormat.BANNER, data.adFormat)
        assertEquals("supplied-banner-unit", data.adUnitId)
        assertEquals("response-banner", data.placement)
    }

    private class RecordingBannerEventCallback : BannerAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }

    private class RecordingBannerRefreshCallback : BannerAdRefreshCallback {
        var refreshedCalled: Boolean = false

        override fun onAdRefreshed() {
            refreshedCalled = true
        }
    }
}
