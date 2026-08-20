@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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
    fun `start installs preload tracking`() {
        val configuration = preloadConfiguration(AD_UNIT_ID)
        val delegate = RecordingPreloadCallback()
        val trackingCallback = slot<PreloadCallback>()
        every { AppOpenAdPreloader.start(PRELOAD_ID, configuration, capture(trackingCallback)) } returns true

        val started = AppOpenAdPreloader.startAndTrack(
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
            expectedFormat = AdFormat.APP_OPEN,
            expectedAdUnitId = AD_UNIT_ID,
            expectedPlacement = START_PLACEMENT,
        )
    }

    @Test
    fun `null poll is returned unchanged`() {
        every { AppOpenAdPreloader.pollAd(PRELOAD_ID) } returns null

        assertNullPoll(AppOpenAdPreloader.pollAndTrackAd(PRELOAD_ID))
    }

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
        private const val START_PLACEMENT = "app-open-start-placement"
        private const val POLL_PLACEMENT = "app-open-poll-placement"
    }
}
