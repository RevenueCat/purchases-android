@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.rewardverification.RewardVerificationResult
import com.revenuecat.purchases.admob.nextgen.show as showWithRewardVerification
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RewardVerificationExtensionsTest {

    @Test
    fun `show delivers failed result as fail-safe when reward verification is not enabled`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var startedCount = 0
        var latestResult: RewardVerificationResult? = null

        ad.showWithRewardVerification(
            activity = activity,
            placement = "placement",
            rewardVerificationStarted = { startedCount++ },
        ) { result ->
            callbackCount++
            latestResult = result
        }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))

        assertEquals(1, callbackCount)
        assertEquals(0, startedCount)
        assertNotNull(latestResult)
        assertTrue(latestResult!!.failed)
    }

    @Test
    fun `show delivers result on main thread`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var callbackThread: Thread? = null

        ad.showWithRewardVerification(activity = activity, placement = "placement") { result ->
            callbackCount++
            callbackThread = Thread.currentThread()
            assertTrue(result.failed)
        }

        val worker = Thread {
            val rewardItem = mockk<RewardItem>(relaxed = true)
            rewardListenerSlot.captured.onUserEarnedReward(rewardItem)
        }
        worker.start()
        worker.join()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callbackCount)
        assertSame(Looper.getMainLooper().thread, callbackThread)
    }

    @Test
    fun `show without placement preserves existing tracked placement`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity) { }

        assertEquals("load-placement", trackingCallback.placement)
    }

    @Test
    fun `show with explicit null placement clears existing tracked placement`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity, placement = null) { }

        assertNull(trackingCallback.placement)
    }

    @Test
    fun `show with placement overrides existing tracked placement`() {
        val ad = mockk<RewardedAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity, placement = "show-placement") { }

        assertEquals("show-placement", trackingCallback.placement)
    }

    @Test
    fun `rewarded interstitial show delivers failed result as fail-safe when reward verification is not enabled`() {
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var startedCount = 0
        var latestResult: RewardVerificationResult? = null

        ad.showWithRewardVerification(
            activity = activity,
            placement = "placement",
            rewardVerificationStarted = { startedCount++ },
        ) { result ->
            callbackCount++
            latestResult = result
        }
        rewardListenerSlot.captured.onUserEarnedReward(mockk<RewardItem>(relaxed = true))

        assertEquals(1, callbackCount)
        assertEquals(0, startedCount)
        assertNotNull(latestResult)
        assertTrue(latestResult!!.failed)
    }

    @Test
    fun `rewarded interstitial show delivers result on main thread`() {
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        var callbackCount = 0
        var callbackThread: Thread? = null

        ad.showWithRewardVerification(activity = activity, placement = "placement") { result ->
            callbackCount++
            callbackThread = Thread.currentThread()
            assertTrue(result.failed)
        }

        val worker = Thread {
            val rewardItem = mockk<RewardItem>(relaxed = true)
            rewardListenerSlot.captured.onUserEarnedReward(rewardItem)
        }
        worker.start()
        worker.join()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callbackCount)
        assertSame(Looper.getMainLooper().thread, callbackThread)
    }

    @Test
    fun `rewarded interstitial show without placement preserves existing tracked placement`() {
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedInterstitialTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity) { }

        assertEquals("load-placement", trackingCallback.placement)
    }

    @Test
    fun `rewarded interstitial show with explicit null placement clears existing tracked placement`() {
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedInterstitialTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity, placement = null) { }

        assertNull(trackingCallback.placement)
    }

    @Test
    fun `rewarded interstitial show with placement overrides existing tracked placement`() {
        val ad = mockk<RewardedInterstitialAd>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val rewardListenerSlot = slot<OnUserEarnedRewardListener>()
        val trackingCallback = rewardedInterstitialTrackingCallback(placement = "load-placement")
        every { ad.adEventCallback } returns trackingCallback
        every { ad.show(activity, capture(rewardListenerSlot)) } answers {}

        ad.showWithRewardVerification(activity = activity, placement = "show-placement") { }

        assertEquals("show-placement", trackingCallback.placement)
    }

    private fun rewardedTrackingCallback(placement: String?): TrackingRewardedAdEventCallback {
        return TrackingRewardedAdEventCallback(
            initialDelegate = null,
            initialPlacement = placement,
            adUnitId = "ad-unit-id",
            responseInfoProvider = { mockk<ResponseInfo>(relaxed = true) },
        )
    }

    private fun rewardedInterstitialTrackingCallback(
        placement: String?,
    ): TrackingRewardedInterstitialAdEventCallback {
        return TrackingRewardedInterstitialAdEventCallback(
            initialDelegate = null,
            initialPlacement = placement,
            adUnitId = "ad-unit-id",
            responseInfoProvider = { mockk<ResponseInfo>(relaxed = true) },
        )
    }
}
