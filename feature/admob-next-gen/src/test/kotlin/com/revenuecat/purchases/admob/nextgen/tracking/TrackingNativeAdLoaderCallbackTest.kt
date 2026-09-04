@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.nextgen.ConfiguredPurchasesRule
import com.revenuecat.purchases.admob.nextgen.responseInfo
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrackingNativeAdLoaderCallbackTest {
    @get:Rule
    val configuredPurchases = ConfiguredPurchasesRule()

    private val adTracker get() = configuredPurchases.adTracker

    @Test
    fun `tracks native load then configures it before delegating`() {
        val nativeAd = mockk<NativeAd>()
        every { nativeAd.getResponseInfo() } returns responseInfo("test-network", "response-id")
        val order = mutableListOf<String>()
        every { adTracker.trackAdLoaded(any(), any()) } answers { order += "track" }
        val callback = TrackingNativeAdLoaderCallback(
            delegate = object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    order += "delegate"
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { order += "configure" },
        )

        callback.onNativeAdLoaded(nativeAd)

        val trackedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.NATIVE,
                placement = "home",
                adUnitId = "ad-unit",
                impressionId = "response-id",
            ),
            trackedData.captured,
        )
        assertEquals(listOf("track", "configure", "delegate"), order)
    }

    @Test
    fun `tracks native failure without configuring and delegates it`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns LoadAdError.ErrorCode.NOT_FOUND
        var delegatedError: LoadAdError? = null
        var configured = false
        val callback = TrackingNativeAdLoaderCallback(
            delegate = object : NativeAdLoaderCallback {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    delegatedError = adError
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            configureAd = { configured = true },
        )

        callback.onAdFailedToLoad(error)

        val trackedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.NATIVE,
                placement = null,
                adUnitId = "ad-unit",
                mediatorErrorCode = 7,
            ),
            trackedData.captured,
        )
        assertSame(error, delegatedError)
        assertFalse("configureAd must not run for an ad that never loaded", configured)
    }

    @Test
    fun `tracks custom native load and delegates without configuring`() {
        val customNativeAd = mockk<CustomNativeAd>()
        every { customNativeAd.getResponseInfo() } returns responseInfo("test-network", "response-id")
        var delegatedCustomNativeAd: CustomNativeAd? = null
        var configured = false
        val callback = TrackingNativeAdLoaderCallback(
            delegate = object : NativeAdLoaderCallback {
                override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
                    delegatedCustomNativeAd = customNativeAd
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { configured = true },
        )

        callback.onCustomNativeAdLoaded(customNativeAd)

        val trackedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.NATIVE,
                placement = "home",
                adUnitId = "ad-unit",
                impressionId = "response-id",
            ),
            trackedData.captured,
        )
        assertSame(customNativeAd, delegatedCustomNativeAd)
        assertFalse(configured)
    }

    @Test
    fun `tracks banner load and delegates without configuring`() {
        val bannerAd = mockk<BannerAd>()
        every { bannerAd.getResponseInfo() } returns responseInfo("test-network", "response-id")
        var delegatedBannerAd: BannerAd? = null
        var configured = false
        val callback = TrackingNativeAdLoaderCallback(
            delegate = object : NativeAdLoaderCallback {
                override fun onBannerAdLoaded(bannerAd: BannerAd) {
                    delegatedBannerAd = bannerAd
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { configured = true },
        )

        callback.onBannerAdLoaded(bannerAd)

        val trackedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home",
                adUnitId = "ad-unit",
                impressionId = "response-id",
            ),
            trackedData.captured,
        )
        assertSame(bannerAd, delegatedBannerAd)
        assertFalse(configured)
    }

    @Test
    fun `forwards loading completion without tracking or configuring`() {
        var loadingCompleted = false
        var configured = false
        val callback = TrackingNativeAdLoaderCallback(
            delegate = object : NativeAdLoaderCallback {
                override fun onAdLoadingCompleted() {
                    loadingCompleted = true
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { configured = true },
        )

        callback.onAdLoadingCompleted()

        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
        assertTrue(loadingCompleted)
        assertFalse(configured)
    }
}
