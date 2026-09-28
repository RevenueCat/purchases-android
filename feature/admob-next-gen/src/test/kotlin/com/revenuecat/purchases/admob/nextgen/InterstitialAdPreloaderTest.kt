@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
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

internal class InterstitialAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(InterstitialAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(InterstitialAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() = assertStartInstallsPreloadTracking(
        expectedAdFormat = AdFormat.INTERSTITIAL,
        stubStart = { preloadId, configuration, callback ->
            every { InterstitialAdPreloader.start(preloadId, configuration, capture(callback)) } returns true
        },
        startAndTrack = { preloadId, configuration, placement, callback ->
            InterstitialAdPreloader.startAndTrack(preloadId, configuration, placement, callback)
        },
    )

    @Test
    fun `null poll is returned unchanged`() = assertNullPollContract(
        stubNullPoll = { every { InterstitialAdPreloader.pollAd(it) } returns null },
        pollAndTrackAd = { InterstitialAdPreloader.pollAndTrackAd(it) },
    )

    @Test
    fun `poll installs lifecycle tracking and returns the same ad`() {
        var installedCallback: InterstitialAdEventCallback? = null
        val interstitialAd = mockk<InterstitialAd>(relaxed = true) {
            every { adUnitId } returns AD_UNIT_ID
            every { getResponseInfo() } returns responseInfo("test-network", "test-response")
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val delegate = mockk<InterstitialAdEventCallback>(relaxed = true)
        every { InterstitialAdPreloader.pollAd(PRELOAD_ID) } returns interstitialAd

        val result = InterstitialAdPreloader.pollAndTrackAd(PRELOAD_ID, POLL_PLACEMENT, delegate)

        assertSame(interstitialAd, result)
        val trackingCallback = installedCallback as TrackingInterstitialAdEventCallback
        assertEquals(POLL_PLACEMENT, trackingCallback.placement)
        assertSame(delegate, trackingCallback.delegate)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }

    companion object {
        private const val PRELOAD_ID = "interstitial-buffer"
        private const val AD_UNIT_ID = "interstitial-unit"
        private const val POLL_PLACEMENT = "interstitial-poll-placement"
    }
}
