@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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

internal class AppOpenAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(AppOpenAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(AppOpenAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() = assertStartInstallsPreloadTracking(
        expectedAdFormat = AdFormat.APP_OPEN,
        stubStart = { preloadId, configuration, callback ->
            every { AppOpenAdPreloader.start(preloadId, configuration, capture(callback)) } returns true
        },
        startAndTrack = { preloadId, configuration, placement, callback ->
            AppOpenAdPreloader.startAndTrack(preloadId, configuration, placement, callback)
        },
    )

    @Test
    fun `null poll is returned unchanged`() = assertNullPollContract(
        stubNullPoll = { every { AppOpenAdPreloader.pollAd(it) } returns null },
        pollAndTrackAd = { AppOpenAdPreloader.pollAndTrackAd(it) },
    )

    @Test
    fun `poll installs lifecycle tracking and returns the same ad`() {
        var installedCallback: AppOpenAdEventCallback? = null
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { adUnitId } returns AD_UNIT_ID
            every { getResponseInfo() } returns responseInfo("test-network", "test-response")
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val delegate = mockk<AppOpenAdEventCallback>(relaxed = true)
        every { AppOpenAdPreloader.pollAd(PRELOAD_ID) } returns appOpenAd

        val result = AppOpenAdPreloader.pollAndTrackAd(PRELOAD_ID, POLL_PLACEMENT, delegate)

        assertSame(appOpenAd, result)
        val trackingCallback = installedCallback as TrackingAppOpenAdEventCallback
        assertEquals(POLL_PLACEMENT, trackingCallback.placement)
        assertSame(delegate, trackingCallback.delegate)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }

    companion object {
        private const val PRELOAD_ID = "app-open-buffer"
        private const val AD_UNIT_ID = "app-open-unit"
        private const val POLL_PLACEMENT = "app-open-poll-placement"
    }
}
