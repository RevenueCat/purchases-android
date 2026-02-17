@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterstitialAdFlowTest {

    @Before
    fun setUp() {
        mockkStatic(InterstitialAd::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(InterstitialAd::class)
    }

    @Test
    fun `interstitial success wires wrappers and forwards callbacks`() {
        val context = mockk<Context>()
        val adRequest = mockk<AdRequest>()
        val interstitialAd = mockk<InterstitialAd>(relaxed = true)
        val responseInfo = mockk<ResponseInfo>()
        every { interstitialAd.responseInfo } returns responseInfo

        val delegateFsc = RecordingFullScreenContentCallback()
        val delegatePaid = RecordingPaidEventListener()
        val loadCallback = RecordingInterstitialLoadCallback()

        val loadCallbackSlot = slot<InterstitialAdLoadCallback>()
        every {
            InterstitialAd.load(any(), any(), any(), capture(loadCallbackSlot))
        } answers {}

        val adTracker = mockk<com.revenuecat.purchases.ads.events.AdTracker>(relaxed = true)
        adTracker.loadAndTrackInterstitialAd(
            context = context,
            adUnitId = "interstitial-unit",
            adRequest = adRequest,
            placement = "home_interstitial",
            loadCallback = loadCallback,
            fullScreenContentCallback = delegateFsc,
            onPaidEventListener = delegatePaid,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdLoaded(interstitialAd)

        assertSame(interstitialAd, loadCallback.loadedAd)

        val fscSlot = slot<FullScreenContentCallback>()
        verify { interstitialAd.fullScreenContentCallback = capture(fscSlot) }
        assertTrue(fscSlot.captured is TrackingFullScreenContentCallback)

        val paidSlot = slot<OnPaidEventListener>()
        verify { interstitialAd.onPaidEventListener = capture(paidSlot) }

        fscSlot.captured.onAdDismissedFullScreenContent()
        assertTrue(delegateFsc.dismissedCalled)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, delegatePaid.lastAdValue)
    }

    @Test
    fun `interstitial failure forwards to load callback`() {
        val context = mockk<Context>()
        val adRequest = mockk<AdRequest>()
        val error = mockk<LoadAdError>()
        val loadCallback = RecordingInterstitialLoadCallback()

        val loadCallbackSlot = slot<InterstitialAdLoadCallback>()
        every {
            InterstitialAd.load(any(), any(), any(), capture(loadCallbackSlot))
        } answers {}

        val adTracker = mockk<com.revenuecat.purchases.ads.events.AdTracker>(relaxed = true)
        adTracker.loadAndTrackInterstitialAd(
            context = context,
            adUnitId = "interstitial-unit",
            adRequest = adRequest,
            placement = "home_interstitial",
            loadCallback = loadCallback,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.failedToLoadError)
    }

    private class RecordingInterstitialLoadCallback : InterstitialAdLoadCallback() {
        var loadedAd: InterstitialAd? = null
        var failedToLoadError: LoadAdError? = null

        override fun onAdLoaded(ad: InterstitialAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            failedToLoadError = error
        }
    }

    private class RecordingFullScreenContentCallback : FullScreenContentCallback() {
        var dismissedCalled: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissedCalled = true
        }
    }

    private class RecordingPaidEventListener : OnPaidEventListener {
        var lastAdValue: AdValue? = null

        override fun onPaidEvent(adValue: AdValue) {
            lastAdValue = adValue
        }
    }
}
