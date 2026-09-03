@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppOpenAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdFlowContract<AppOpenAd, AppOpenAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(AdFormat.APP_OPEN, "app-open-unit", "load-placement"),
        suspendingValues = FullScreenAdTestValues(
            AdFormat.APP_OPEN,
            "suspend-app-open-unit",
            "suspend-load-placement",
        ),
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
        val initialEventCallback = RecordingAppOpenAdEventCallback()
        val replacementEventCallback = RecordingAppOpenAdEventCallback()

        contract.callbackSuccess(
            createAd = { responseInfo, installedCallback ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback } answers { installedCallback.callback }
                    every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                }
            },
            initialEventCallback = initialEventCallback,
            replacementEventCallback = replacementEventCallback,
            stubLoad = { adRequest, trackingLoadCallback ->
                every { AppOpenAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback, eventCallback ->
                adTracker.loadAndTrackAppOpenAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                    adEventCallback = eventCallback,
                )
            },
            asTrackingCallback = { it as? TrackingAppOpenAdEventCallback },
            invokeDelegateCallback = { it.onAdDismissedFullScreenContent() },
            assertInitialDelegateInvoked = { assertTrue(initialEventCallback.dismissed) },
            setTrackingEventCallback = { ad, eventCallback -> ad.setTrackingAdEventCallback(eventCallback) },
            assertReplacementDelegateInvoked = { assertTrue(replacementEventCallback.dismissed) },
            show = { ad, activity, placement -> ad.show(activity, placement) },
            verifyShow = { ad, activity -> verify(exactly = 1) { ad.show(activity) } },
        )
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

        assertShowClearsPlacement(
            trackingCallback = trackingCallback,
            show = { appOpenAd.show(activity, placement = null) },
            verifyShow = { verify(exactly = 1) { appOpenAd.show(activity) } },
        )
    }

    @Test
    fun `app open failure is forwarded to load callback`() {
        contract.callbackFailure(
            stubLoad = { adRequest, trackingLoadCallback ->
                every { AppOpenAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback ->
                adTracker.loadAndTrackAppOpenAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    @Test
    fun `suspending app open success tracks and installs callback before returning original result`() = runBlocking {
        val eventCallback = RecordingAppOpenAdEventCallback()

        contract.suspendingSuccess(
            createAd = { responseInfo, installedCallback ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback } answers { installedCallback.callback }
                    every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                }
            },
            eventCallback = eventCallback,
            stubLoad = { adRequest, sdkResult -> coEvery { AppOpenAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest, delegate ->
                adTracker.loadAndTrackAppOpenAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingAppOpenAdEventCallback },
            invokeDelegateCallback = { it.onAdDismissedFullScreenContent() },
            assertDelegateInvoked = { assertTrue(eventCallback.dismissed) },
        )
    }

    @Test
    fun `suspending app open failure tracks error and returns original result`() = runBlocking {
        contract.suspendingFailure(
            stubLoad = { adRequest, sdkResult -> coEvery { AppOpenAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest ->
                adTracker.loadAndTrackAppOpenAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                )
            },
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val eventCallback = RecordingAppOpenAdEventCallback()

        contract.eventCallbackFallback(
            createAd = {
                mockk(relaxed = true) {
                    every { adEventCallback } returns null
                }
            },
            eventCallback = eventCallback,
            setTrackingEventCallback = { ad, callback -> ad.setTrackingAdEventCallback(callback) },
            verifyEventCallbackInstalled = { ad, callback ->
                verify(exactly = 1) { ad.adEventCallback = callback }
            },
        )
    }

    private class RecordingAppOpenAdEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }
}
