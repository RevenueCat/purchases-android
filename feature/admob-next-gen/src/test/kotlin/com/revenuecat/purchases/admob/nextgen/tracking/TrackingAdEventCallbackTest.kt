@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
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
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
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
        every { purchases.adTracker } returns adTracker
        every { responseInfo.adapterClassName } returns "test-network"
        every { responseInfo.responseId } returns "response-id"
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    /**
     * The six format callbacks differ only in the [AdFormat] and [AdDisplayedTrigger] they hand to
     * the base class, so a copy-paste slip is the likely failure. Driving all of them here catches
     * both a wrong format and a wrong trigger, the latter of which would double-count displays for
     * full-screen formats since the SDK fires onAdImpression alongside onAdShowedFullScreenContent.
     */
    @Test
    fun `each format tracks display from its own trigger only`() {
        displayTrackingCases().forEach { case ->
            val caseTracker = mockk<AdTracker>(relaxed = true)
            every { purchases.adTracker } returns caseTracker
            val callback = case.create { responseInfo }

            when (case.displayTrigger) {
                AdDisplayedTrigger.IMPRESSION -> callback.onAdShowedFullScreenContent()
                AdDisplayedTrigger.FULL_SCREEN_SHOW -> callback.onAdImpression()
            }

            verify(exactly = 0) { caseTracker.trackAdDisplayed(any(), any()) }

            when (case.displayTrigger) {
                AdDisplayedTrigger.IMPRESSION -> callback.onAdImpression()
                AdDisplayedTrigger.FULL_SCREEN_SHOW -> callback.onAdShowedFullScreenContent()
            }

            val trackedData = slot<AdDisplayedData>()
            verify(exactly = 1) {
                caseTracker.trackAdDisplayed(capture(trackedData), AdCaptureMethod.ADAPTER)
            }
            assertEquals(case.description, case.adFormat, trackedData.captured.adFormat)
        }
    }

    @Test
    fun `display tracking covers every format callback`() {
        val covered = displayTrackingCases().map { it.create { responseInfo }.javaClass }.toSet()

        assertEquals(trackingEventCallbacksBySdkInterface.values.toSet(), covered)
    }

    private class DisplayTrackingCase(
        val description: String,
        val adFormat: AdFormat,
        val displayTrigger: AdDisplayedTrigger,
        val create: (() -> ResponseInfo) -> AdEventCallback,
    )

    private fun displayTrackingCases() = listOf(
        DisplayTrackingCase("banner", AdFormat.BANNER, AdDisplayedTrigger.IMPRESSION) {
            TrackingBannerAdEventCallback(null, "home", "ad-unit", it)
        },
        DisplayTrackingCase("native", AdFormat.NATIVE, AdDisplayedTrigger.IMPRESSION) {
            TrackingNativeAdEventCallback(null, "home", "ad-unit", it)
        },
        DisplayTrackingCase("interstitial", AdFormat.INTERSTITIAL, AdDisplayedTrigger.FULL_SCREEN_SHOW) {
            TrackingInterstitialAdEventCallback(null, "home", "ad-unit", it)
        },
        DisplayTrackingCase("app open", AdFormat.APP_OPEN, AdDisplayedTrigger.FULL_SCREEN_SHOW) {
            TrackingAppOpenAdEventCallback(null, "home", "ad-unit", it)
        },
        DisplayTrackingCase("rewarded", AdFormat.REWARDED, AdDisplayedTrigger.FULL_SCREEN_SHOW) {
            TrackingRewardedAdEventCallback(null, "home", "ad-unit", it)
        },
        DisplayTrackingCase(
            "rewarded interstitial",
            AdFormat.REWARDED_INTERSTITIAL,
            AdDisplayedTrigger.FULL_SCREEN_SHOW,
        ) {
            TrackingRewardedInterstitialAdEventCallback(null, "home", "ad-unit", it)
        },
    )

    @Test
    fun `tracks click before forwarding callback`() {
        val order = mutableListOf<String>()
        every { adTracker.trackAdOpened(any(), any()) } answers { order += "track" }
        val delegate = object : InterstitialAdEventCallback {
            override fun onAdClicked() {
                order += "delegate"
            }
        }
        val callback = TrackingInterstitialAdEventCallback(delegate, "home", "ad-unit") { responseInfo }

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
        val callback = TrackingBannerAdEventCallback(delegate, "load-placement", "ad-unit") { responseInfo }
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
    fun `refreshed banner reads current response info at event time`() {
        val refreshedResponseInfo = mockk<ResponseInfo>()
        every { refreshedResponseInfo.adapterClassName } returns "refreshed-network"
        every { refreshedResponseInfo.responseId } returns "refreshed-response"
        var currentResponseInfo = responseInfo
        val callback = TrackingBannerAdEventCallback(null, "home", "ad-unit") { currentResponseInfo }
        currentResponseInfo = refreshedResponseInfo

        callback.onAdImpression()
        callback.onAdClicked()
        callback.onAdPaid(AdValue(PrecisionType.PRECISE, 50_000L, "USD"))

        val displayedData = slot<AdDisplayedData>()
        val openedData = slot<AdOpenedData>()
        val revenueData = slot<AdRevenueData>()
        verify(exactly = 1) {
            adTracker.trackAdDisplayed(capture(displayedData), AdCaptureMethod.ADAPTER)
            adTracker.trackAdOpened(capture(openedData), AdCaptureMethod.ADAPTER)
            adTracker.trackAdRevenue(capture(revenueData), AdCaptureMethod.ADAPTER)
        }
        listOf(
            displayedData.captured.networkName to displayedData.captured.impressionId,
            openedData.captured.networkName to openedData.captured.impressionId,
            revenueData.captured.networkName to revenueData.captured.impressionId,
        ).forEach {
            assertEquals("refreshed-network" to "refreshed-response", it)
        }
    }

    @Test
    fun `unconfigured Purchases skips tracking but still delegates`() {
        every { Purchases.isConfigured } returns false
        var delegated = false
        val callback = TrackingBannerAdEventCallback(
            delegate = object : BannerAdEventCallback {
                override fun onAdImpression() {
                    delegated = true
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfoProvider = { responseInfo },
        )

        callback.onAdImpression()

        verify(exactly = 0) { adTracker.trackAdDisplayed(any(), any()) }
        assertEquals(true, delegated)
    }
}
