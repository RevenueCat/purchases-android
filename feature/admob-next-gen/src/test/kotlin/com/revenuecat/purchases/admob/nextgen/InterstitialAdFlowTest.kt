@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InterstitialAdFlowTest {

    @Before
    fun setUp() {
        mockkObject(InterstitialAd.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(InterstitialAd.Companion)
    }

    @Test
    fun `interstitial success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "interstitial-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
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

        mockk<AdTracker>().loadAndTrackInterstitialAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(interstitialAd)

        assertSame(interstitialAd, loadCallback.loadedAd)
        val trackingCallback = installedCallback as TrackingInterstitialAdEventCallback
        trackingCallback.onAppEvent("name", "data")
        assertTrue(initialEventCallback.appEventCalled)

        interstitialAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAppEvent("name", "data")
        assertTrue(replacementEventCallback.appEventCalled)

        interstitialAd.show(activity, "show-placement")
        assertSame("show-placement", trackingCallback.placement)
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

        mockk<AdTracker>().loadAndTrackInterstitialAd(
            adRequest = adRequest,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
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
