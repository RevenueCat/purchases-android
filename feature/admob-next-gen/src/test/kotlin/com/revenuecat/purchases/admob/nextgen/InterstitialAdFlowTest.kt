@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class InterstitialAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(InterstitialAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(InterstitialAd.Companion)
    }

    @Test
    fun `interstitial success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "interstitial-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        var installedCallback: InterstitialAdEventCallback? = null
        val interstitialAd = mockk<InterstitialAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val loadCallback = RecordingInterstitialLoadCallback()
        val initialEventCallback = RecordingInterstitialAdEventCallback()
        val replacementEventCallback = RecordingInterstitialAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<InterstitialAd>>()

        every { InterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackInterstitialAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(interstitialAd)

        assertSame(interstitialAd, loadCallback.loadedAd)

        // Pins the format, ad unit and placement this entry point hands to the load tracker;
        // the wrapper classes are covered separately, so only the wiring is asserted here.
        val loadedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "load-placement",
                adUnitId = "interstitial-unit",
                impressionId = "response-id",
            ),
            loadedData.captured,
        )
        val trackingCallback = installedCallback as TrackingInterstitialAdEventCallback
        trackingCallback.onAppEvent("name", "data")
        assertTrue(initialEventCallback.appEventCalled)

        interstitialAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAppEvent("name", "data")
        assertTrue(replacementEventCallback.appEventCalled)

        interstitialAd.show(activity, "show-placement")
        assertEquals("show-placement", trackingCallback.placement)
        verify(exactly = 1) { interstitialAd.show(activity) }
    }

    @Test
    fun `show with null placement clears load-time placement`() {
        val activity = mockk<Activity>()
        val trackingCallback = TrackingInterstitialAdEventCallback(
            initialDelegate = null,
            initialPlacement = "load-placement",
            adUnitId = "interstitial-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )
        val interstitialAd = mockk<InterstitialAd>(relaxed = true) {
            every { adEventCallback } returns trackingCallback
        }

        interstitialAd.show(activity, placement = null)

        assertNull(trackingCallback.placement)
        verify(exactly = 1) { interstitialAd.show(activity) }
    }

    @Test
    fun `interstitial failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "interstitial-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingInterstitialLoadCallback()
        val trackingLoadCallback = slot<AdLoadCallback<InterstitialAd>>()

        every { InterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackInterstitialAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)

        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.INTERSTITIAL, failedData.captured.adFormat)
        assertEquals("interstitial-unit", failedData.captured.adUnitId)
        assertEquals("load-placement", failedData.captured.placement)
    }

    @Test
    fun `suspending interstitial success tracks and installs callback before returning original result`() =
        runBlocking {
            val adRequest = mockk<AdRequest> {
                every { adUnitId } returns "suspend-interstitial-unit"
            }
            val responseInfo = mockk<ResponseInfo>(relaxed = true) {
                every { adapterClassName } returns "suspend-test-network"
                every { responseId } returns "suspend-response-id"
            }
            var installedCallback: InterstitialAdEventCallback? = null
            val interstitialAd = mockk<InterstitialAd>(relaxed = true) {
                every { getResponseInfo() } returns responseInfo
                every { adEventCallback } answers { installedCallback }
                every { adEventCallback = any() } answers { installedCallback = firstArg() }
            }
            val eventCallback = RecordingInterstitialAdEventCallback()
            val sdkResult = AdLoadResult.Success(interstitialAd)

            coEvery { InterstitialAd.load(adRequest) } returns sdkResult

            val result = adTracker.awaitLoadAndTrackInterstitialAd(
                adRequest = adRequest,
                placement = "suspend-load-placement",
                adEventCallback = eventCallback,
            )

            assertSame(sdkResult, result)
            val trackingCallback = installedCallback as TrackingInterstitialAdEventCallback
            trackingCallback.onAppEvent("name", "data")
            assertTrue(eventCallback.appEventCalled)

            val loadedData = slot<AdLoadedData>()
            verify(exactly = 1) {
                adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
            }
            assertEquals(
                AdLoadedData(
                    networkName = "suspend-test-network",
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = AdFormat.INTERSTITIAL,
                    placement = "suspend-load-placement",
                    adUnitId = "suspend-interstitial-unit",
                    impressionId = "suspend-response-id",
                ),
                loadedData.captured,
            )
        }

    @Test
    fun `suspending interstitial failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "suspend-interstitial-unit"
        }
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val sdkResult = AdLoadResult.Failure<InterstitialAd>(error)

        coEvery { InterstitialAd.load(adRequest) } returns sdkResult

        val result = adTracker.awaitLoadAndTrackInterstitialAd(
            adRequest = adRequest,
            placement = "suspend-load-placement",
        )

        assertSame(sdkResult, result)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "suspend-load-placement",
                adUnitId = "suspend-interstitial-unit",
                mediatorErrorCode = LoadAdError.ErrorCode.NETWORK_ERROR.value,
            ),
            failedData.captured,
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val interstitialAd = mockk<InterstitialAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingInterstitialAdEventCallback()

        interstitialAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { interstitialAd.adEventCallback = eventCallback }
    }

    private class RecordingInterstitialAdEventCallback : InterstitialAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }

    private class RecordingInterstitialLoadCallback : AdLoadCallback<InterstitialAd> {
        var loadedAd: InterstitialAd? = null
        var loadError: LoadAdError? = null

        override fun onAdLoaded(ad: InterstitialAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }
    }
}
