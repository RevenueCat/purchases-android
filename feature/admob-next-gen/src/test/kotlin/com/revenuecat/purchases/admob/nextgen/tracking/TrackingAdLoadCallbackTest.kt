@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackingAdLoadCallbackTest {
    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        PurchasesTestHelper.setSharedInstance(purchases)
    }

    @After
    fun tearDown() {
        PurchasesTestHelper.setSharedInstance(null)
    }

    @Test
    fun `tracks loaded ad then configures it before delegating`() {
        val responseInfo = responseInfo(adapterClassName = "test-network", responseId = "response-id")
        val ad = mockk<Ad>()
        every { ad.getResponseInfo() } returns responseInfo
        val order = mutableListOf<String>()
        every { adTracker.trackAdLoaded(any(), any()) } answers { order += "track" }
        val delegate = object : AdLoadCallback<Ad> {
            override fun onAdLoaded(ad: Ad) {
                order += "delegate"
            }
        }
        val callback = TrackingAdLoadCallback(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { order += "configure" },
        )

        callback.onAdLoaded(ad)

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
        assertEquals(listOf("track", "configure", "delegate"), order)
    }

    @Test
    fun `tracks numeric SDK error value and delegates failure`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns LoadAdError.ErrorCode.NOT_FOUND
        var delegatedError: LoadAdError? = null
        val callback = TrackingAdLoadCallback<Ad>(
            delegate = object : AdLoadCallback<Ad> {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    delegatedError = adError
                }
            },
            adFormat = AdFormat.INTERSTITIAL,
            placement = null,
            adUnitId = "ad-unit",
            configureAd = {},
        )

        callback.onAdFailedToLoad(error)

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
        assertEquals(error, delegatedError)
    }

    @Test
    fun `still configures and delegates when Purchases is not configured`() {
        PurchasesTestHelper.setSharedInstance(null)
        val ad = mockk<Ad>()
        every { ad.getResponseInfo() } returns responseInfo("test-network", "response-id")
        val order = mutableListOf<String>()
        val callback = TrackingAdLoadCallback(
            delegate = object : AdLoadCallback<Ad> {
                override fun onAdLoaded(ad: Ad) {
                    order += "delegate"
                }
            },
            adFormat = AdFormat.BANNER,
            placement = "home",
            adUnitId = "ad-unit",
            configureAd = { order += "configure" },
        )

        callback.onAdLoaded(ad)

        verify(exactly = 0) { adTracker.trackAdLoaded(any(), any()) }
        assertEquals(listOf("configure", "delegate"), order)
    }

    private fun responseInfo(adapterClassName: String, responseId: String): ResponseInfo =
        mockk<ResponseInfo>().also {
            every { it.adapterClassName } returns adapterClassName
            every { it.responseId } returns responseId
        }
}
