@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen.tracking

import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackingAdEventCallbackTest {
    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val responseInfo = mockk<ResponseInfo>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { purchases.adTracker } returns adTracker
        every { responseInfo.adapterClassName } returns "test-network"
        every { responseInfo.responseId } returns "response-id"
        PurchasesTestHelper.setSharedInstance(purchases)
    }

    @After
    fun tearDown() {
        PurchasesTestHelper.setSharedInstance(null)
        unmockkStatic(Log::class)
    }

    @Test
    fun `banner ignores full screen show and tracks display from impression`() {
        val callback = TrackingBannerAdEventCallback(null, "home", "ad-unit", responseInfo)

        callback.onAdShowedFullScreenContent()

        verify(exactly = 0) { adTracker.trackAdDisplayed(any(), any()) }

        callback.onAdImpression()

        val trackedData = slot<AdDisplayedData>()
        verify(exactly = 1) {
            adTracker.trackAdDisplayed(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.BANNER, trackedData.captured.adFormat)
    }

    @Test
    fun `full screen ad ignores impression and tracks display from show`() {
        val callback = TrackingInterstitialAdEventCallback(null, "home", "ad-unit", responseInfo)

        callback.onAdImpression()

        verify(exactly = 0) { adTracker.trackAdDisplayed(any(), any()) }

        callback.onAdShowedFullScreenContent()

        val trackedData = slot<AdDisplayedData>()
        verify(exactly = 1) {
            adTracker.trackAdDisplayed(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.INTERSTITIAL, trackedData.captured.adFormat)
    }

    @Test
    fun `tracks click before forwarding callback`() {
        val order = mutableListOf<String>()
        every { adTracker.trackAdOpened(any(), any()) } answers { order += "track" }
        val delegate = object : InterstitialAdEventCallback {
            override fun onAdClicked() {
                order += "delegate"
            }
        }
        val callback = TrackingInterstitialAdEventCallback(delegate, "home", "ad-unit", responseInfo)

        callback.onAdClicked()

        val trackedData = slot<AdOpenedData>()
        verify(exactly = 1) {
            adTracker.trackAdOpened(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdOpenedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "home",
                adUnitId = "ad-unit",
                impressionId = "response-id",
            ),
            trackedData.captured,
        )
        assertEquals(listOf("track", "delegate"), order)
    }

    @Test
    fun `paid callback uses placement at event time and forwards`() {
        var delegatedValue: AdValue? = null
        val delegate = object : BannerAdEventCallback {
            override fun onAdPaid(value: AdValue) {
                delegatedValue = value
            }
        }
        val callback = TrackingBannerAdEventCallback(delegate, "load-placement", "ad-unit", responseInfo)
        callback.placement = "show-placement"
        val value = AdValue(PrecisionType.PUBLISHER_PROVIDED, 50_000L, "USD")

        callback.onAdPaid(value)

        val trackedData = slot<AdRevenueData>()
        verify(exactly = 1) {
            adTracker.trackAdRevenue(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdRevenueData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "show-placement",
                adUnitId = "ad-unit",
                impressionId = "response-id",
                revenueMicros = 50_000L,
                currency = "USD",
                precision = AdRevenuePrecision.PUBLISHER_DEFINED,
            ),
            trackedData.captured,
        )
        assertEquals(value, delegatedValue)
    }

    @Test
    fun `unconfigured Purchases skips tracking but still delegates`() {
        PurchasesTestHelper.setSharedInstance(null)
        var delegated = false
        val callback = TrackingBannerAdEventCallback(
            delegate = object : BannerAdEventCallback {
                override fun onAdImpression() {
                    delegated = true
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfo = responseInfo,
        )

        callback.onAdImpression()

        verify(exactly = 0) { adTracker.trackAdDisplayed(any(), any()) }
        assertEquals(true, delegated)
    }
}
