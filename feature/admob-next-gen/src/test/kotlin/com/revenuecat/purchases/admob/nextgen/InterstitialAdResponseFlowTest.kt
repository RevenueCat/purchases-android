@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterstitialAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdResponseFlowContract<InterstitialAd, InterstitialAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(
            adFormat = AdFormat.INTERSTITIAL,
            adUnitId = "supplied-interstitial-unit",
            placement = "response-interstitial",
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
    fun `response success tracks and installs event callback before forwarding`() {
        val eventCallback = RecordingInterstitialEventCallback()

        contract.responseSuccess(
            createAd = { responseInfo, installedCallback, onCallbackInstalled ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback = any() } answers {
                        installedCallback.callback = firstArg()
                        onCallbackInstalled()
                    }
                }
            },
            eventCallback = eventCallback,
            stubLoadFromResponse = { trackingLoadCallback ->
                every { InterstitialAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback, delegate ->
                adTracker.loadAndTrackInterstitialAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-interstitial-unit",
                    placement = "response-interstitial",
                    loadCallback = loadCallback,
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingInterstitialAdEventCallback },
            invokeDelegateCallback = { it.onAppEvent("name", "data") },
            assertDelegateInvoked = { assertTrue(eventCallback.appEventCalled) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure(
            stubLoadFromResponse = { trackingLoadCallback ->
                every { InterstitialAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback ->
                adTracker.loadAndTrackInterstitialAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-interstitial-unit",
                    placement = "response-interstitial",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    private class RecordingInterstitialEventCallback : InterstitialAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }
}
