@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class AppOpenAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(AppOpenAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(AppOpenAd.Companion)
    }

    @Test
    fun `response success tracks and installs event callback before forwarding`() {
        val order = mutableListOf<String>()
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
        }
        val loadCallback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                order += "load-callback"
                assertSame(appOpenAd, ad)
            }
        }
        val eventCallback = RecordingAppOpenEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()
        val installedEventCallback = slot<AppOpenAdEventCallback>()
        val loadedData = slot<AdLoadedData>()

        every {
            AppOpenAd.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }
        every { appOpenAd.adEventCallback = capture(installedEventCallback) } answers {
            order += "event-callback"
        }

        adTracker.loadAndTrackAppOpenAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-app-open-unit",
            placement = "response-app-open",
            loadCallback = loadCallback,
            adEventCallback = eventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(appOpenAd)

        assertEquals(listOf("tracked", "event-callback", "load-callback"), order)
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.APP_OPEN,
                placement = "response-app-open",
                adUnitId = "supplied-app-open-unit",
                impressionId = "response-id",
            ),
            loadedData.captured,
        )
        assertTrue(installedEventCallback.captured is TrackingAppOpenAdEventCallback)

        installedEventCallback.captured.onAdDismissedFullScreenContent()
        assertTrue(eventCallback.dismissed)
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        val order = mutableListOf<String>()
        val error = LoadAdError(
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
            "invalid response",
            mockk(relaxed = true),
        )
        val loadCallback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                order += "load-callback"
                assertSame(error, adError)
            }
        }
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()
        val failedData = slot<AdFailedToLoadData>()

        every {
            AppOpenAd.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs
        every { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        adTracker.loadAndTrackAppOpenAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-app-open-unit",
            placement = "response-app-open",
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertEquals(listOf("tracked", "load-callback"), order)
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.APP_OPEN, failedData.captured.adFormat)
        assertEquals("supplied-app-open-unit", failedData.captured.adUnitId)
        assertEquals("response-app-open", failedData.captured.placement)
    }

    private class RecordingAppOpenEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }
}
