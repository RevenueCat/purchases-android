@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class NativeAdResponseFlowTest {
    @get:Rule
    val configuredPurchases = ConfiguredPurchasesRule()

    private val adTracker get() = configuredPurchases.adTracker

    @Before
    fun setUp() {
        mockkObject(NativeAdLoader.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(NativeAdLoader.Companion)
    }

    @Test
    fun `response success uses supplied attribution and configures before forwarding`() {
        val order = mutableListOf<String>()
        var installedCallback: NativeAdEventCallback? = null
        val loadedAd = mockk<NativeAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo("native-network", "native-response")
            every { adEventCallback } answers { installedCallback }
        }
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()
        val loadedData = slot<AdLoadedData>()
        val delegate = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                order += "delegate"
                assertSame(loadedAd, nativeAd)
                assertTrue(nativeAd.adEventCallback is TrackingNativeAdEventCallback)
            }
        }
        every {
            NativeAdLoader.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "track"
        }
        every { loadedAd.adEventCallback = any() } answers {
            installedCallback = firstArg()
            order += "configure"
        }

        adTracker.loadAndTrackNativeAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-native-unit",
            placement = "feed",
            loadCallback = delegate,
        )
        trackingLoadCallback.captured.onNativeAdLoaded(loadedAd)

        assertEquals(listOf("track", "configure", "delegate"), order)
        assertEquals(AdFormat.NATIVE, loadedData.captured.adFormat)
        assertEquals("feed", loadedData.captured.placement)
        assertEquals("supplied-native-unit", loadedData.captured.adUnitId)
    }

    @Test
    fun `response banner success installs event and refresh tracking before forwarding`() {
        var installedEventCallback: BannerAdEventCallback? = null
        var installedRefreshCallback: BannerAdRefreshCallback? = null
        val loadedAd = mockk<BannerAd>(relaxed = true) {
            every { getResponseInfo() } returns mockk<ResponseInfo>(relaxed = true)
            every { adEventCallback } answers { installedEventCallback }
            every { adEventCallback = any() } answers { installedEventCallback = firstArg() }
            every { bannerAdRefreshCallback } answers { installedRefreshCallback }
            every { bannerAdRefreshCallback = any() } answers { installedRefreshCallback = firstArg() }
        }
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()
        val eventDelegate = mockk<BannerAdEventCallback>(relaxed = true)
        val delegate = object : NativeAdLoaderCallback {
            override fun onBannerAdLoaded(bannerAd: BannerAd) {
                assertSame(loadedAd, bannerAd)
                val eventCallback = bannerAd.adEventCallback as TrackingBannerAdEventCallback
                val refreshCallback = bannerAd.bannerAdRefreshCallback as TrackingBannerAdRefreshCallback
                assertSame(eventDelegate, eventCallback.delegate)
                assertNull(refreshCallback.delegate)
            }
        }
        every {
            NativeAdLoader.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs

        adTracker.loadAndTrackNativeAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-banner-unit",
            placement = "feed",
            loadCallback = delegate,
            bannerAdEventCallback = eventDelegate,
        )
        trackingLoadCallback.captured.onBannerAdLoaded(loadedAd)
    }

    @Test
    fun `response failure uses supplied attribution before forwarding`() {
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.INVALID_AD_RESPONSE
        }
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()
        var forwardedError: LoadAdError? = null
        val delegate = object : NativeAdLoaderCallback {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                forwardedError = adError
            }
        }
        every {
            NativeAdLoader.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs

        adTracker.loadAndTrackNativeAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-native-unit",
            placement = "feed",
            loadCallback = delegate,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, forwardedError)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.NATIVE, failedData.captured.adFormat)
        assertEquals("feed", failedData.captured.placement)
        assertEquals("supplied-native-unit", failedData.captured.adUnitId)
    }
}
