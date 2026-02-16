@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RewardedAdFlowTest {

    @Before
    fun setUp() {
        mockkStatic(RewardedAd::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(RewardedAd::class)
    }

    @Test
    fun `rewarded success wires wrappers and forwards callbacks`() {
        val context = mockk<Context>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val rewardedAd = mockk<RewardedAd>(relaxed = true)
        val responseInfo = mockk<ResponseInfo>()
        every { rewardedAd.responseInfo } returns responseInfo

        val delegateFsc = RecordingFullScreenContentCallback()
        val delegatePaid = RecordingPaidEventListener()
        val loadCallback = RecordingRewardedLoadCallback()

        val loadCallbackSlot = slot<RewardedAdLoadCallback>()
        every {
            RewardedAd.load(any<Context>(), any<String>(), any<AdRequest>(), capture(loadCallbackSlot))
        } answers {}

        loadAndTrackRewardedAdInternal(
            context = context,
            adUnitId = "rewarded-unit",
            adRequest = adRequest,
            placement = "rewarded",
            loadCallback = loadCallback,
            fullScreenContentCallback = delegateFsc,
            onPaidEventListener = delegatePaid,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdLoaded(rewardedAd)

        assertSame(rewardedAd, loadCallback.loadedAd)
        val fscSlot = slot<FullScreenContentCallback>()
        verify { rewardedAd.fullScreenContentCallback = capture(fscSlot) }
        assertTrue(fscSlot.captured is TrackingFullScreenContentCallback)

        val paidSlot = slot<OnPaidEventListener>()
        verify { rewardedAd.onPaidEventListener = capture(paidSlot) }

        fscSlot.captured.onAdDismissedFullScreenContent()
        assertTrue(delegateFsc.dismissedCalled)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, delegatePaid.lastAdValue)
    }

    @Test
    fun `rewarded failure forwards to load callback`() {
        val context = mockk<Context>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val error = mockk<LoadAdError>()
        val loadCallback = RecordingRewardedLoadCallback()

        val loadCallbackSlot = slot<RewardedAdLoadCallback>()
        every {
            RewardedAd.load(any<Context>(), any<String>(), any<AdRequest>(), capture(loadCallbackSlot))
        } answers {}

        loadAndTrackRewardedAdInternal(
            context = context,
            adUnitId = "rewarded-unit",
            adRequest = adRequest,
            placement = "rewarded",
            loadCallback = loadCallback,
        )

        assertNotNull(loadCallbackSlot.captured)
        loadCallbackSlot.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.failedToLoadError)
    }

    private class RecordingRewardedLoadCallback : RewardedAdLoadCallback() {
        var loadedAd: RewardedAd? = null
        var failedToLoadError: LoadAdError? = null

        override fun onAdLoaded(ad: RewardedAd) {
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
