@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
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
internal class RewardedAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(RewardedAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(RewardedAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() {
        val configuration = preloadConfiguration(AD_UNIT_ID)
        val delegate = RecordingPreloadCallback()
        val trackingCallback = slot<PreloadCallback>()
        every { RewardedAdPreloader.start(PRELOAD_ID, configuration, capture(trackingCallback)) } returns true

        val started = RewardedAdPreloader.startAndTrack(
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
            expectedFormat = AdFormat.REWARDED,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = START_PLACEMENT,
        )
    }

    @Test
    fun `null poll is returned unchanged`() {
        every { RewardedAdPreloader.pollAd(PRELOAD_ID) } returns null

        assertNullPoll(RewardedAdPreloader.pollAndTrackAd(PRELOAD_ID))
    }

    @Test
    fun `poll installs lifecycle tracking and returns the same ad`() {
        var installedCallback: RewardedAdEventCallback? = null
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { adUnitId } returns AD_UNIT_ID
            every { getResponseInfo() } returns responseInfo("test-network", "test-response")
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val delegate = mockk<RewardedAdEventCallback>(relaxed = true)
        every { RewardedAdPreloader.pollAd(PRELOAD_ID) } returns rewardedAd

        val result = RewardedAdPreloader.pollAndTrackAd(PRELOAD_ID, POLL_PLACEMENT, delegate)

        assertSame(rewardedAd, result)
        val trackingCallback = installedCallback as TrackingRewardedAdEventCallback
        assertEquals(POLL_PLACEMENT, trackingCallback.placement)
        assertSame(delegate, trackingCallback.delegate)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }

    companion object {
        private const val PRELOAD_ID = "rewarded-buffer"
        private const val AD_UNIT_ID = "rewarded-unit"
        private const val START_PLACEMENT = "rewarded-start-placement"
        private const val POLL_PLACEMENT = "rewarded-poll-placement"
    }
}
