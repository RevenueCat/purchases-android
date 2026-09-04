@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
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

    /**
     * [TrackingAdEventCallback.delegate] is a var so show-time code can swap the application
     * callback after the ad was loaded. Both an inherited override and a format-specific one are
     * exercised, since the format classes reach the property through their own overrides and would
     * otherwise be free to hold on to the delegate they were constructed with.
     */
    @Test
    fun `swapping the delegate redirects inherited and format callbacks`() {
        val seen = mutableListOf<String>()
        val callback = TrackingBannerAdEventCallback(
            initialDelegate = recordingBannerDelegate("original", seen),
            initialPlacement = "home",
            adUnitId = "ad-unit",
            responseInfoProvider = { responseInfo },
        )

        callback.delegate = recordingBannerDelegate("replacement", seen)
        callback.onAdClicked()
        callback.onAppEvent("event", null)

        assertEquals(listOf("replacement.onAdClicked", "replacement.onAppEvent"), seen)
    }

    @Test
    fun `clearing the delegate stops forwarding but keeps tracking`() {
        val seen = mutableListOf<String>()
        val callback = TrackingBannerAdEventCallback(
            initialDelegate = recordingBannerDelegate("original", seen),
            initialPlacement = "home",
            adUnitId = "ad-unit",
            responseInfoProvider = { responseInfo },
        )

        callback.delegate = null
        callback.onAdClicked()
        callback.onAppEvent("event", null)

        assertEquals(emptyList<String>(), seen)
        verify(exactly = 1) { adTracker.trackAdOpened(any(), AdCaptureMethod.ADAPTER) }
    }

    /**
     * The paid callback covers this for revenue; display and click read the same var, so a
     * placement captured at construction instead of at event time would only show up here.
     */
    @Test
    fun `placement is read at event time for display and click events`() {
        val callback = TrackingBannerAdEventCallback(null, "load-placement", "ad-unit") { responseInfo }
        callback.placement = "show-placement"

        callback.onAdImpression()
        callback.onAdClicked()

        val displayedData = slot<AdDisplayedData>()
        val openedData = slot<AdOpenedData>()
        verify(exactly = 1) {
            adTracker.trackAdDisplayed(capture(displayedData), AdCaptureMethod.ADAPTER)
            adTracker.trackAdOpened(capture(openedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals("show-placement", displayedData.captured.placement)
        assertEquals("show-placement", openedData.captured.placement)
    }

    private fun recordingBannerDelegate(name: String, seen: MutableList<String>) =
        object : BannerAdEventCallback {
            override fun onAdClicked() {
                seen += "$name.onAdClicked"
            }

            override fun onAppEvent(eventName: String, data: String?) {
                seen += "$name.onAppEvent"
            }
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
            initialDelegate = object : BannerAdEventCallback {
                override fun onAdImpression() {
                    delegated = true
                }
            },
            initialPlacement = null,
            adUnitId = "ad-unit",
            responseInfoProvider = { responseInfo },
        )

        callback.onAdImpression()

        verify(exactly = 0) { adTracker.trackAdDisplayed(any(), any()) }
        assertEquals(true, delegated)
    }
}
