@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.responseInfo
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackingBannerAdRefreshCallbackTest {
    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    @Test
    fun `refresh reads current response info then tracks and delegates`() {
        val initialResponseInfo = responseInfo("initial-network", "initial-response")
        val refreshedResponseInfo = responseInfo("refreshed-network", "refreshed-response")
        var currentResponseInfo = initialResponseInfo
        val order = mutableListOf<String>()
        every { adTracker.trackAdLoaded(any(), any()) } answers { order += "track" }
        val callback = TrackingBannerAdRefreshCallback(
            delegate = object : BannerAdRefreshCallback {
                override fun onAdRefreshed() {
                    order += "delegate"
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            responseInfoProvider = { currentResponseInfo },
        )
        currentResponseInfo = refreshedResponseInfo

        callback.onAdRefreshed()

        val trackedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "refreshed-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home",
                adUnitId = "ad-unit",
                impressionId = "refreshed-response",
            ),
            trackedData.captured,
        )
        assertEquals(listOf("track", "delegate"), order)
    }

    @Test
    fun `failed refresh tracks numeric SDK error value then delegates`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns LoadAdError.ErrorCode.NOT_FOUND
        var delegatedError: LoadAdError? = null
        val callback = TrackingBannerAdRefreshCallback(
            delegate = object : BannerAdRefreshCallback {
                override fun onAdFailedToRefresh(adError: LoadAdError) {
                    delegatedError = adError
                }
            },
            placement = null,
            adUnitId = "ad-unit",
            responseInfoProvider = { mockk() },
        )

        callback.onAdFailedToRefresh(error)

        val trackedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = null,
                adUnitId = "ad-unit",
                mediatorErrorCode = 7,
            ),
            trackedData.captured,
        )
        assertEquals(error, delegatedError)
    }

    @Test
    fun `still delegates refresh when Purchases is not configured`() {
        every { Purchases.isConfigured } returns false
        var delegated = false
        val callback = TrackingBannerAdRefreshCallback(
            delegate = object : BannerAdRefreshCallback {
                override fun onAdRefreshed() {
                    delegated = true
                }
            },
            placement = "home",
            adUnitId = "ad-unit",
            responseInfoProvider = { responseInfo("test-network", "response-id") },
        )

        callback.onAdRefreshed()

        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
        assertTrue(delegated)
    }
}
