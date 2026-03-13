@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.AdTracker
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RewardedInterstitialAdFlowTest {

    @Before
    fun setUp() {
        mockkStatic(RewardedInterstitialAd::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(RewardedInterstitialAd::class)
    }

    @Test
    fun `rewarded interstitial success wires wrappers and forwards callbacks`() {
        val context = mockk<Context>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true)
        val responseInfo = mockk<ResponseInfo>()
        every { rewardedInterstitialAd.responseInfo } returns responseInfo

        val delegateFsc = RecordingFullScreenContentCallback()
        val delegatePaid = RecordingPaidEventListener()
        val loadCallback = RecordingRewardedInterstitialLoadCallback()

        val loadCallbackSlot = slot<RewardedInterstitialAdLoadCallback>()
        every {
            RewardedInterstitialAd.load(any<Context>(), any<String>(), any<AdRequest>(), capture(loadCallbackSlot))
        } answers {}

        val adTracker = mockk<AdTracker>(relaxed = true)
        adTracker.loadAndTrackRewardedInterstitialAd(
            context = context,
            adUnitId = "rewarded-interstitial-unit",
            adRequest = adRequest,
            placement = "rewarded_interstitial",
            loadCallback = loadCallback,
            fullScreenContentCallback = delegateFsc,
            onPaidEventListener = delegatePaid,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdLoaded(rewardedInterstitialAd)

        assertSame(rewardedInterstitialAd, loadCallback.loadedAd)
        val fscSlot = slot<FullScreenContentCallback>()
        verify { rewardedInterstitialAd.fullScreenContentCallback = capture(fscSlot) }
        assertTrue(fscSlot.captured is TrackingFullScreenContentCallback)

        val paidSlot = slot<OnPaidEventListener>()
        verify { rewardedInterstitialAd.onPaidEventListener = capture(paidSlot) }

        fscSlot.captured.onAdDismissedFullScreenContent()
        assertTrue(delegateFsc.dismissedCalled)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, delegatePaid.lastAdValue)
    }

    @Test
    fun `rewarded interstitial failure forwards to load callback`() {
        val context = mockk<Context>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val error = mockk<LoadAdError>()
        val loadCallback = RecordingRewardedInterstitialLoadCallback()

        val loadCallbackSlot = slot<RewardedInterstitialAdLoadCallback>()
        every {
            RewardedInterstitialAd.load(any<Context>(), any<String>(), any<AdRequest>(), capture(loadCallbackSlot))
        } answers {}

        val adTracker = mockk<AdTracker>(relaxed = true)
        adTracker.loadAndTrackRewardedInterstitialAd(
            context = context,
            adUnitId = "rewarded-interstitial-unit",
            adRequest = adRequest,
            placement = "rewarded_interstitial",
            loadCallback = loadCallback,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.failedToLoadError)
    }

    private class RecordingRewardedInterstitialLoadCallback : RewardedInterstitialAdLoadCallback() {
        var loadedAd: RewardedInterstitialAd? = null
        var failedToLoadError: LoadAdError? = null

        override fun onAdLoaded(ad: RewardedInterstitialAd) {
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
