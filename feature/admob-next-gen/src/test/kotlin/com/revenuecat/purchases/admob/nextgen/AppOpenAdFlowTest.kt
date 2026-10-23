@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppOpenAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val values = FullScreenAdTestValues(AdFormat.APP_OPEN, "app-open-unit", "load-placement")
    private val suspendingValues = FullScreenAdTestValues(
        AdFormat.APP_OPEN,
        "suspend-app-open-unit",
        "suspend-load-placement",
    )

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
            every { adUnitId } returns values.adUnitId
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
        val loadCallback = FullScreenRecordingAdLoadCallback<AppOpenAd>()
        val initialEventCallback = RecordingAppOpenAdEventCallback()
        val replacementEventCallback = RecordingAppOpenAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(appOpenAd)

        assertSame(appOpenAd, loadCallback.loadedAd)
        adTracker.assertLoadedData(slot(), values, "test-network", "response-id")

        val trackingCallback = requireNotNull(installedCallback as? TrackingAppOpenAdEventCallback)
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
            every { adUnitId } returns values.adUnitId
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = FullScreenRecordingAdLoadCallback<AppOpenAd>()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
        adTracker.assertFailedData(slot(), values)
    }

    @Test
    fun `suspending app open success tracks and installs callback before returning original result`() = runBlocking {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns suspendingValues.adUnitId
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

        val result = adTracker.loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = suspendingValues.placement,
            adEventCallback = eventCallback,
        )

        assertSame(sdkResult, result)
        val trackingCallback = requireNotNull(installedCallback as? TrackingAppOpenAdEventCallback)
        trackingCallback.onAdDismissedFullScreenContent()
        assertTrue(eventCallback.dismissed)
        adTracker.assertLoadedData(
            slot<AdLoadedData>(),
            suspendingValues,
            "suspend-test-network",
            "suspend-response-id",
        )
    }

    @Test
    fun `suspending app open failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns suspendingValues.adUnitId
        }
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val sdkResult = AdLoadResult.Failure<AppOpenAd>(error)

        coEvery { AppOpenAd.load(adRequest) } returns sdkResult

        val result = adTracker.loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = suspendingValues.placement,
        )

        assertSame(sdkResult, result)
        adTracker.assertSuspendingFailedData(slot<AdFailedToLoadData>(), suspendingValues)
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
}
