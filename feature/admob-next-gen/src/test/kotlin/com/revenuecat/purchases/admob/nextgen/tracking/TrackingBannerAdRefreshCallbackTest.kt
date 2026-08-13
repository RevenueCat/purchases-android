@file:OptIn(
    ExperimentalPreviewRevenueCatPurchasesAPI::class,
    InternalRevenueCatAPI::class,
)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
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

class TrackingBannerAdRefreshCallbackTest {
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

    private fun responseInfo(adapterClassName: String, responseId: String): ResponseInfo =
        mockk<ResponseInfo>().also {
            every { it.adapterClassName } returns adapterClassName
            every { it.responseId } returns responseId
        }
}
