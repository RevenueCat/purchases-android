@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.ads.events.AdCaptureMethod
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class RewardedAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(RewardedAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(RewardedAd.Companion)
    }

    @Test
    fun `response success tracks and installs event callback before forwarding`() {
        val order = mutableListOf<String>()
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
        }
        val loadCallback = object : AdLoadCallback<RewardedAd> {
            override fun onAdLoaded(ad: RewardedAd) {
                order += "load-callback"
                assertSame(rewardedAd, ad)
            }
        }
        val eventCallback = RecordingRewardedEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()
        val installedEventCallback = slot<RewardedAdEventCallback>()
        val loadedData = slot<AdLoadedData>()

        every {
            RewardedAd.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs
        every { adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }
        every { rewardedAd.adEventCallback = capture(installedEventCallback) } answers {
            order += "event-callback"
        }

        adTracker.loadAndTrackRewardedAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-rewarded-unit",
            placement = "response-rewarded",
            loadCallback = loadCallback,
            adEventCallback = eventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedAd)

        assertEquals(listOf("tracked", "event-callback", "load-callback"), order)
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "response-rewarded",
                adUnitId = "supplied-rewarded-unit",
                impressionId = "response-id",
            ),
            loadedData.captured,
        )
        assertTrue(installedEventCallback.captured is TrackingRewardedAdEventCallback)

        installedEventCallback.captured.onAdMetadataChanged()
        assertTrue(eventCallback.metadataChangedCalled)
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        val order = mutableListOf<String>()
        val error = LoadAdError(
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
            "invalid response",
            mockk(relaxed = true),
        )
        val loadCallback = object : AdLoadCallback<RewardedAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                order += "load-callback"
                assertSame(error, adError)
            }
        }
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()
        val failedData = slot<AdFailedToLoadData>()

        every {
            RewardedAd.loadFromAdResponse("opaque-response", capture(trackingLoadCallback))
        } just runs
        every { adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER) } answers {
            order += "tracked"
        }

        adTracker.loadAndTrackRewardedAdFromResponse(
            adResponse = "opaque-response",
            adUnitId = "supplied-rewarded-unit",
            placement = "response-rewarded",
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertEquals(listOf("tracked", "load-callback"), order)
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.REWARDED, failedData.captured.adFormat)
        assertEquals("supplied-rewarded-unit", failedData.captured.adUnitId)
        assertEquals("response-rewarded", failedData.captured.placement)
    }

    private class RecordingRewardedEventCallback : RewardedAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
