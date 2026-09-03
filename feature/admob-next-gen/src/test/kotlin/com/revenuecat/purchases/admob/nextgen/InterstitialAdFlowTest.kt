@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
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

class InterstitialAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdFlowContract<InterstitialAd, InterstitialAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(AdFormat.INTERSTITIAL, "interstitial-unit", "load-placement"),
        suspendingValues = FullScreenAdTestValues(
            AdFormat.INTERSTITIAL,
            "suspend-interstitial-unit",
            "suspend-load-placement",
        ),
    )

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
        val initialEventCallback = RecordingInterstitialAdEventCallback()
        val replacementEventCallback = RecordingInterstitialAdEventCallback()

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
                every { InterstitialAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback, eventCallback ->
                adTracker.loadAndTrackInterstitialAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                    adEventCallback = eventCallback,
                )
            },
            asTrackingCallback = { it as? TrackingInterstitialAdEventCallback },
            invokeDelegateCallback = { it.onAppEvent("name", "data") },
            assertInitialDelegateInvoked = { assertTrue(initialEventCallback.appEventCalled) },
            setTrackingEventCallback = { ad, eventCallback -> ad.setTrackingAdEventCallback(eventCallback) },
            assertReplacementDelegateInvoked = { assertTrue(replacementEventCallback.appEventCalled) },
            show = { ad, activity, placement -> ad.show(activity, placement) },
            verifyShow = { ad, activity -> verify(exactly = 1) { ad.show(activity) } },
        )
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

        assertShowClearsPlacement(
            trackingCallback = trackingCallback,
            show = { interstitialAd.show(activity, placement = null) },
            verifyShow = { verify(exactly = 1) { interstitialAd.show(activity) } },
        )
    }

    @Test
    fun `interstitial failure is forwarded to load callback`() {
        contract.callbackFailure(
            stubLoad = { adRequest, trackingLoadCallback ->
                every { InterstitialAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback ->
                adTracker.loadAndTrackInterstitialAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    @Test
    fun `suspending interstitial success tracks and installs callback before returning original result`() = runBlocking {
        val eventCallback = RecordingInterstitialAdEventCallback()

        contract.suspendingSuccess(
            createAd = { responseInfo, installedCallback ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback } answers { installedCallback.callback }
                    every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                }
            },
            eventCallback = eventCallback,
            stubLoad = { adRequest, sdkResult -> coEvery { InterstitialAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest, delegate ->
                adTracker.loadAndTrackInterstitialAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingInterstitialAdEventCallback },
            invokeDelegateCallback = { it.onAppEvent("name", "data") },
            assertDelegateInvoked = { assertTrue(eventCallback.appEventCalled) },
        )
    }

    @Test
    fun `suspending interstitial failure tracks error and returns original result`() = runBlocking {
        contract.suspendingFailure(
            stubLoad = { adRequest, sdkResult -> coEvery { InterstitialAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest ->
                adTracker.loadAndTrackInterstitialAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                )
            },
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val eventCallback = RecordingInterstitialAdEventCallback()

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

    private class RecordingInterstitialAdEventCallback : InterstitialAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }
}
