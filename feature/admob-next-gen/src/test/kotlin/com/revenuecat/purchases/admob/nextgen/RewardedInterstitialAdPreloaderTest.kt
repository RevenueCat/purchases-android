@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
internal class RewardedInterstitialAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(RewardedInterstitialAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(RewardedInterstitialAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() {
        val configuration = preloadConfiguration(AD_UNIT_ID)
        val delegate = RecordingPreloadCallback()
        val trackingCallback = slot<PreloadCallback>()
        every {
            RewardedInterstitialAdPreloader.start(PRELOAD_ID, configuration, capture(trackingCallback))
        } returns true

        val started = RewardedInterstitialAdPreloader.startAndTrack(
            preloadId = PRELOAD_ID,
            preloadConfiguration = configuration,
            placement = START_PLACEMENT,
            preloadCallback = delegate,
        )

        assertTrue(started)
        assertSuccessfulPreload(
            preloadId = PRELOAD_ID,
            trackingCallback = trackingCallback.captured,
            delegate = delegate,
            expectedFormat = AdFormat.REWARDED_INTERSTITIAL,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = START_PLACEMENT,
        )
    }

    @Test
    fun `null poll is returned unchanged`() {
        every { RewardedInterstitialAdPreloader.pollAd(PRELOAD_ID) } returns null

        assertNullPoll(RewardedInterstitialAdPreloader.pollAndTrackAd(PRELOAD_ID))
    }

    @Test
    fun `poll installs lifecycle tracking and returns the same ad`() {
        var installedCallback: RewardedInterstitialAdEventCallback? = null
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { adUnitId } returns AD_UNIT_ID
            every { getResponseInfo() } returns responseInfo("test-network", "test-response")
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val delegate = mockk<RewardedInterstitialAdEventCallback>(relaxed = true)
        every { RewardedInterstitialAdPreloader.pollAd(PRELOAD_ID) } returns rewardedInterstitialAd

        val result = RewardedInterstitialAdPreloader.pollAndTrackAd(PRELOAD_ID, POLL_PLACEMENT, delegate)

        assertSame(rewardedInterstitialAd, result)
        val trackingCallback = installedCallback as TrackingRewardedInterstitialAdEventCallback
        assertEquals(POLL_PLACEMENT, trackingCallback.placement)
        assertSame(delegate, trackingCallback.delegate)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }

    companion object {
        private const val PRELOAD_ID = "rewarded-interstitial-buffer"
        private const val AD_UNIT_ID = "rewarded-interstitial-unit"
        private const val START_PLACEMENT = "rewarded-interstitial-start-placement"
        private const val POLL_PLACEMENT = "rewarded-interstitial-poll-placement"
    }
}
