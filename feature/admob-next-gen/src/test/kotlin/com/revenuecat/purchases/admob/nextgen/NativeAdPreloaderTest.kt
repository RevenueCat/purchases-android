@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
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
import org.junit.Before
import org.junit.Test
internal class NativeAdPreloaderTest : PreloaderTest() {

    @Before
    fun setUpPreloader() {
        mockkObject(NativeAdPreloader.Companion)
    }

    @After
    fun tearDownPreloader() {
        unmockkObject(NativeAdPreloader.Companion)
    }

    @Test
    fun `start installs preload tracking`() = assertStartInstallsPreloadTracking(
        expectedAdFormat = AdFormat.NATIVE,
        stubStart = { preloadId, configuration, callback ->
            every { NativeAdPreloader.start(preloadId, configuration, capture(callback)) } returns true
        },
        startAndTrack = { preloadId, configuration, placement, callback ->
            NativeAdPreloader.startAndTrack(preloadId, configuration, placement, callback)
        },
    )

    @Test
    fun `null poll is returned unchanged`() = assertNullPollContract(
        stubNullPoll = { every { NativeAdPreloader.pollAd(it) } returns null },
        pollAndTrackAd = { NativeAdPreloader.pollAndTrackAd(it) },
    )

    @Test
    fun `poll installs callbacks for every success result without tracking load`() {
        val responseInfo = responseInfo("native-network", "native-response")
        var installedNativeCallback: NativeAdEventCallback? = null
        var installedCustomNativeCallback: NativeAdEventCallback? = null
        var installedBannerCallback: BannerAdEventCallback? = null
        var installedBannerRefreshCallback: BannerAdRefreshCallback? = null
        val nativeAd = mockk<NativeAd>(relaxed = true) {
            every { adUnitId } returns "native-unit"
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers { installedNativeCallback = firstArg() }
        }
        val customNativeAd = mockk<CustomNativeAd>(relaxed = true) {
            every { adUnitId } returns "custom-native-unit"
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers { installedCustomNativeCallback = firstArg() }
        }
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { adUnitId } returns "banner-unit"
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers { installedBannerCallback = firstArg() }
            every { bannerAdRefreshCallback = any() } answers { installedBannerRefreshCallback = firstArg() }
        }
        val nativeResult = NativeAdLoadResult.NativeAdSuccess(nativeAd)
        val customNativeResult = NativeAdLoadResult.CustomNativeAdSuccess(customNativeAd)
        val bannerResult = NativeAdLoadResult.BannerAdSuccess(bannerAd)
        val nativeDelegate = mockk<NativeAdEventCallback>(relaxed = true)
        val bannerDelegate = mockk<BannerAdEventCallback>(relaxed = true)
        val refreshDelegate = mockk<BannerAdRefreshCallback>(relaxed = true)
        every { NativeAdPreloader.pollAd("native-buffer") } returns nativeResult
        every { NativeAdPreloader.pollAd("custom-native-buffer") } returns customNativeResult
        every { NativeAdPreloader.pollAd("native-banner-buffer") } returns bannerResult

        val polledNativeResult = NativeAdPreloader.pollAndTrackAd(
            preloadId = "native-buffer",
            placement = "native-poll-placement",
            nativeAdEventCallback = nativeDelegate,
        )
        val polledCustomNativeResult = NativeAdPreloader.pollAndTrackAd(
            preloadId = "custom-native-buffer",
            placement = "custom-native-poll-placement",
            nativeAdEventCallback = nativeDelegate,
        )
        val polledBannerResult = NativeAdPreloader.pollAndTrackAd(
            preloadId = "native-banner-buffer",
            placement = "native-banner-poll-placement",
            bannerAdEventCallback = bannerDelegate,
            bannerAdRefreshCallback = refreshDelegate,
        )

        assertSame(nativeResult, polledNativeResult)
        assertSame(customNativeResult, polledCustomNativeResult)
        assertSame(bannerResult, polledBannerResult)
        val nativeTrackingCallback = installedNativeCallback as TrackingNativeAdEventCallback
        val customNativeTrackingCallback = installedCustomNativeCallback as TrackingNativeAdEventCallback
        val bannerTrackingCallback = installedBannerCallback as TrackingBannerAdEventCallback
        val bannerRefreshTrackingCallback = installedBannerRefreshCallback as TrackingBannerAdRefreshCallback
        assertEquals("native-poll-placement", nativeTrackingCallback.placement)
        assertEquals("custom-native-poll-placement", customNativeTrackingCallback.placement)
        assertEquals("native-banner-poll-placement", bannerTrackingCallback.placement)
        assertSame(nativeDelegate, nativeTrackingCallback.delegate)
        assertSame(nativeDelegate, customNativeTrackingCallback.delegate)
        assertSame(bannerDelegate, bannerTrackingCallback.delegate)
        assertSame(refreshDelegate, bannerRefreshTrackingCallback.delegate)
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
    }
}
