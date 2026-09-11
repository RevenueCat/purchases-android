@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun `start installs preload tracking`() = assertStartInstallsPreloadTracking(
        expectedAdFormat = AdFormat.REWARDED,
        stubStart = { preloadId, configuration, callback ->
            every { RewardedAdPreloader.start(preloadId, configuration, capture(callback)) } returns true
        },
        startAndTrack = { preloadId, configuration, placement, callback ->
            RewardedAdPreloader.startAndTrack(preloadId, configuration, placement, callback)
        },
    )

    @Test
    fun `null poll is returned unchanged`() = assertNullPollContract(
        stubNullPoll = { every { RewardedAdPreloader.pollAd(it) } returns null },
        pollAndTrackAd = { RewardedAdPreloader.pollAndTrackAd(it) },
    )

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
        private const val POLL_PLACEMENT = "rewarded-poll-placement"
    }
}
