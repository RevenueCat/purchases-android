@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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
class AppOpenAdFlowTest {

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
    fun `app open success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "app-open-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        var installedCallback: AppOpenAdEventCallback? = null
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val loadCallback = RecordingAppOpenLoadCallback()
        val initialEventCallback = RecordingAppOpenAdEventCallback()
        val replacementEventCallback = RecordingAppOpenAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(appOpenAd)

        assertSame(appOpenAd, loadCallback.loadedAd)

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
                adFormat = AdFormat.APP_OPEN,
                placement = "load-placement",
                adUnitId = "app-open-unit",
                impressionId = "response-id",
            ),
            loadedData.captured,
        )
        val trackingCallback = installedCallback as TrackingAppOpenAdEventCallback
        trackingCallback.onAdDismissedFullScreenContent()
        assertTrue(initialEventCallback.dismissed)

        appOpenAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdDismissedFullScreenContent()
        assertTrue(replacementEventCallback.dismissed)

        appOpenAd.show(activity, "show-placement")
        assertEquals("show-placement", trackingCallback.placement)
        verify(exactly = 1) { appOpenAd.show(activity) }
    }

    @Test
    fun `show with null placement clears load-time placement`() {
        val activity = mockk<Activity>()
        val trackingCallback = TrackingAppOpenAdEventCallback(
            initialDelegate = null,
            initialPlacement = "load-placement",
            adUnitId = "app-open-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { adEventCallback } returns trackingCallback
        }

        appOpenAd.show(activity, placement = null)

        assertNull(trackingCallback.placement)
        verify(exactly = 1) { appOpenAd.show(activity) }
    }

    @Test
    fun `app open failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "app-open-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingAppOpenLoadCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackAppOpenAd(
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
        assertEquals(AdFormat.APP_OPEN, failedData.captured.adFormat)
        assertEquals("app-open-unit", failedData.captured.adUnitId)
        assertEquals("load-placement", failedData.captured.placement)
    }

    @Test
    fun `suspending app open success tracks and installs callback before returning original result`() =
        runBlocking {
            val adRequest = mockk<AdRequest> {
                every { adUnitId } returns "suspend-app-open-unit"
            }
            val responseInfo = mockk<ResponseInfo>(relaxed = true) {
                every { adapterClassName } returns "suspend-test-network"
                every { responseId } returns "suspend-response-id"
            }
            var installedCallback: AppOpenAdEventCallback? = null
            val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
                every { getResponseInfo() } returns responseInfo
                every { adEventCallback } answers { installedCallback }
                every { adEventCallback = any() } answers { installedCallback = firstArg() }
            }
            val eventCallback = RecordingAppOpenAdEventCallback()
            val sdkResult = AdLoadResult.Success(appOpenAd)

            coEvery { AppOpenAd.load(adRequest) } returns sdkResult

            val result = adTracker.awaitLoadAndTrackAppOpenAd(
                adRequest = adRequest,
                placement = "suspend-load-placement",
                adEventCallback = eventCallback,
            )

            assertSame(sdkResult, result)
            val trackingCallback = installedCallback as TrackingAppOpenAdEventCallback
            trackingCallback.onAdDismissedFullScreenContent()
            assertTrue(eventCallback.dismissed)

            val loadedData = slot<AdLoadedData>()
            verify(exactly = 1) {
                adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
            }
            assertEquals(
                AdLoadedData(
                    networkName = "suspend-test-network",
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = AdFormat.APP_OPEN,
                    placement = "suspend-load-placement",
                    adUnitId = "suspend-app-open-unit",
                    impressionId = "suspend-response-id",
                ),
                loadedData.captured,
            )
        }

    @Test
    fun `suspending app open failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "suspend-app-open-unit"
        }
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val sdkResult = AdLoadResult.Failure<AppOpenAd>(error)

        coEvery { AppOpenAd.load(adRequest) } returns sdkResult

        val result = adTracker.awaitLoadAndTrackAppOpenAd(
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
                adFormat = AdFormat.APP_OPEN,
                placement = "suspend-load-placement",
                adUnitId = "suspend-app-open-unit",
                mediatorErrorCode = LoadAdError.ErrorCode.NETWORK_ERROR.value,
            ),
            failedData.captured,
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingAppOpenAdEventCallback()

        appOpenAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { appOpenAd.adEventCallback = eventCallback }
    }

    private class RecordingAppOpenAdEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }

    private class RecordingAppOpenLoadCallback : AdLoadCallback<AppOpenAd> {
        var loadedAd: AppOpenAd? = null
        var loadError: LoadAdError? = null

        override fun onAdLoaded(ad: AppOpenAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }
    }
}
