@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdPreloader
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
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

internal class BannerAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(BannerAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(BannerAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() {
        val configuration = preloadConfiguration("banner-unit")
        val delegate = RecordingPreloadCallback()
        val trackingCallback = slot<PreloadCallback>()
        every { BannerAdPreloader.start("banner-buffer", configuration, capture(trackingCallback)) } returns true

        val started = BannerAdPreloader.startAndTrack(
            preloadId = "banner-buffer",
            preloadConfiguration = configuration,
            placement = "banner-start-placement",
            preloadCallback = delegate,
        )

        assertTrue(started)
        assertSuccessfulPreload(
            preloadId = "banner-buffer",
            trackingCallback = trackingCallback.captured,
            delegate = delegate,
            expectedFormat = AdFormat.BANNER,
            expectedAdUnitId = "banner-unit",
            expectedPlacement = "banner-start-placement",
        )
    }

    @Test
    fun `null poll is returned unchanged`() {
        every { BannerAdPreloader.pollAd("banner-buffer") } returns null

        assertNullPoll(BannerAdPreloader.pollAndTrackAd("banner-buffer"))
    }

    @Test
    fun `poll installs lifecycle and refresh tracking with independent placement and no load event`() {
        val responseInfo = responseInfo("banner-network", "banner-response")
        var installedEventCallback: BannerAdEventCallback? = null
        var installedRefreshCallback: BannerAdRefreshCallback? = null
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { adUnitId } returns "banner-unit"
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers { installedEventCallback = firstArg() }
            every { bannerAdRefreshCallback = any() } answers { installedRefreshCallback = firstArg() }
        }
        val eventDelegate = RecordingBannerAdEventCallback()
        val refreshDelegate = RecordingBannerAdRefreshCallback()
        val refreshError = loadError(LoadAdError.ErrorCode.NO_FILL)
        every { BannerAdPreloader.pollAd("banner-buffer") } returns bannerAd

        val result = BannerAdPreloader.pollAndTrackAd(
            preloadId = "banner-buffer",
            placement = "banner-poll-placement",
            adEventCallback = eventDelegate,
            bannerAdRefreshCallback = refreshDelegate,
        )

        assertSame(bannerAd, result)
        val trackingEventCallback = installedEventCallback as TrackingBannerAdEventCallback
        val trackingRefreshCallback = installedRefreshCallback as TrackingBannerAdRefreshCallback
        assertEquals("banner-poll-placement", trackingEventCallback.placement)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }

        trackingEventCallback.onAdImpression()
        trackingRefreshCallback.onAdFailedToRefresh(refreshError)

        assertTrue(eventDelegate.impressionCalled)
        assertSame(refreshError, refreshDelegate.refreshError)
        val displayedData = slot<AdDisplayedData>()
        verify(exactly = 1) { adTracker.trackAdDisplayed(capture(displayedData), AdCaptureMethod.ADAPTER) }
        assertEquals("banner-poll-placement", displayedData.captured.placement)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) }
        assertEquals("banner-poll-placement", failedData.captured.placement)
    }

    private class RecordingBannerAdEventCallback : BannerAdEventCallback {
        var impressionCalled = false

        override fun onAdImpression() {
            impressionCalled = true
        }
    }

    private class RecordingBannerAdRefreshCallback : BannerAdRefreshCallback {
        var refreshError: LoadAdError? = null

        override fun onAdFailedToRefresh(adError: LoadAdError) {
            refreshError = adError
        }
    }
}
