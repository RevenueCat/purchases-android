@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class AdLoadResultTrackingTest {
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
    fun `success tracks loaded ad before configuring and returns original result`() {
        val responseInfo = responseInfo(adapterClassName = "test-network", responseId = "response-id")
        val ad = mockk<Ad>()
        every { ad.getResponseInfo() } returns responseInfo
        val order = mutableListOf<String>()
        every { adTracker.trackAdLoaded(any(), any()) } answers { order += "track" }
        val result = AdLoadResult.Success(ad)

        val returnedResult = result.trackAndConfigureAdLoadResult(
            adFormat = AdFormat.BANNER,
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { order += "configure" },
        )

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
        assertEquals(listOf("track", "configure"), order)
        assertSame(result, returnedResult)
    }

    @Test
    fun `failure tracks failed load without configuring and returns original result`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns LoadAdError.ErrorCode.NOT_FOUND
        var configured = false
        val result = AdLoadResult.Failure<Ad>(error)

        val returnedResult = result.trackAndConfigureAdLoadResult(
            adFormat = AdFormat.INTERSTITIAL,
            placement = null,
            adUnitId = "ad-unit",
            configureAd = { configured = true },
        )

        val trackedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(trackedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = null,
                adUnitId = "ad-unit",
                mediatorErrorCode = 7,
            ),
            trackedData.captured,
        )
        assertFalse("configureAd must not run for a failed load", configured)
        assertSame(result, returnedResult)
    }

    @Test
    fun `success still configures when Purchases is not configured`() {
        every { Purchases.isConfigured } returns false
        val ad = mockk<Ad>()
        var configuredAd: Ad? = null
        val result = AdLoadResult.Success(ad)

        result.trackAndConfigureAdLoadResult(
            adFormat = AdFormat.REWARDED,
            placement = "bonus",
            adUnitId = "ad-unit",
            configureAd = { configuredAd = it },
        )

        verify(exactly = 0) { ad.getResponseInfo() }
        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
        assertSame(ad, configuredAd)
    }

    @Test
    fun `success still configures when reading response info throws`() {
        val ad = mockk<Ad>()
        every { ad.getResponseInfo() } throws IllegalStateException("boom")
        var configuredAd: Ad? = null

        AdLoadResult.Success(ad).trackAndConfigureAdLoadResult(
            adFormat = AdFormat.APP_OPEN,
            placement = null,
            adUnitId = "ad-unit",
            configureAd = { configuredAd = it },
        )

        assertSame(ad, configuredAd)
    }

    @Test
    fun `success still configures when tracking throws`() {
        every { adTracker.trackAdLoaded(any(), any()) } throws IllegalStateException("boom")
        val ad = mockk<Ad>()
        every { ad.getResponseInfo() } returns responseInfo("test-network", "response-id")
        var configuredAd: Ad? = null

        AdLoadResult.Success(ad).trackAndConfigureAdLoadResult(
            adFormat = AdFormat.REWARDED_INTERSTITIAL,
            placement = "level-end",
            adUnitId = "ad-unit",
            configureAd = { configuredAd = it },
        )

        assertSame(ad, configuredAd)
    }
}
