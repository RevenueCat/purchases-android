@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class RewardedAdFlowTest {

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
    fun `rewarded success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        var installedCallback: RewardedAdEventCallback? = null
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val loadCallback = RecordingRewardedLoadCallback()
        val initialEventCallback = RecordingRewardedAdEventCallback()
        val replacementEventCallback = RecordingRewardedAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()

        every { RewardedAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedAd)

        assertSame(rewardedAd, loadCallback.loadedAd)

        // Pins the format, ad unit and placement this entry point hands to the load tracker;
        // the wrapper classes are covered separately, so only the wiring is asserted here.
        val loadedData = slot<AdLoadedData>()
        verify(exactly = 1) {
            adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdLoadedData(
                networkName = "test-network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "load-placement",
                adUnitId = "rewarded-unit",
                impressionId = "response-id",
            ),
            loadedData.captured,
        )
        val trackingCallback = installedCallback as TrackingRewardedAdEventCallback
        trackingCallback.onAdMetadataChanged()
        assertTrue(initialEventCallback.metadataChangedCalled)

        rewardedAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdMetadataChanged()
        assertTrue(replacementEventCallback.metadataChangedCalled)

        rewardedAd.show(activity, "show-placement", rewardListener)
        assertEquals("show-placement", trackingCallback.placement)
        verify(exactly = 1) { rewardedAd.show(activity, rewardListener) }
    }

    @Test
    fun `show with null placement clears load-time placement`() {
        val trackingCallback = TrackingRewardedAdEventCallback(
            initialDelegate = null,
            initialPlacement = "load-placement",
            adUnitId = "rewarded-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { adEventCallback } returns trackingCallback
        }
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()

        rewardedAd.show(
            activity = activity,
            placement = null,
            onUserEarnedRewardListener = rewardListener,
        )

        assertNull(trackingCallback.placement)
        verify(exactly = 1) { rewardedAd.show(activity, rewardListener) }
    }

    @Test
    fun `rewarded failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingRewardedLoadCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()

        every { RewardedAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)

        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(AdFormat.REWARDED, failedData.captured.adFormat)
        assertEquals("rewarded-unit", failedData.captured.adUnitId)
        assertEquals("load-placement", failedData.captured.placement)
    }

    @Test
    fun `suspending rewarded success tracks and installs callback before returning original result`() =
        runBlocking {
            val adRequest = mockk<AdRequest> {
                every { adUnitId } returns "suspend-rewarded-unit"
            }
            val responseInfo = mockk<ResponseInfo>(relaxed = true) {
                every { adapterClassName } returns "suspend-test-network"
                every { responseId } returns "suspend-response-id"
            }
            var installedCallback: RewardedAdEventCallback? = null
            val rewardedAd = mockk<RewardedAd>(relaxed = true) {
                every { getResponseInfo() } returns responseInfo
                every { adEventCallback } answers { installedCallback }
                every { adEventCallback = any() } answers { installedCallback = firstArg() }
            }
            val eventCallback = RecordingRewardedAdEventCallback()
            val sdkResult = AdLoadResult.Success(rewardedAd)

            coEvery { RewardedAd.load(adRequest) } returns sdkResult

            val result = adTracker.loadAndTrackRewardedAd(
                adRequest = adRequest,
                placement = "suspend-load-placement",
                adEventCallback = eventCallback,
            )

            assertSame(sdkResult, result)
            val trackingCallback = installedCallback as TrackingRewardedAdEventCallback
            trackingCallback.onAdMetadataChanged()
            assertTrue(eventCallback.metadataChangedCalled)

            val loadedData = slot<AdLoadedData>()
            verify(exactly = 1) {
                adTracker.trackAdLoaded(capture(loadedData), AdCaptureMethod.ADAPTER)
            }
            assertEquals(
                AdLoadedData(
                    networkName = "suspend-test-network",
                    mediatorName = AdMediatorName.AD_MOB,
                    adFormat = AdFormat.REWARDED,
                    placement = "suspend-load-placement",
                    adUnitId = "suspend-rewarded-unit",
                    impressionId = "suspend-response-id",
                ),
                loadedData.captured,
            )
        }

    @Test
    fun `suspending rewarded failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "suspend-rewarded-unit"
        }
        val error = mockk<LoadAdError> {
            every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR
        }
        val sdkResult = AdLoadResult.Failure<RewardedAd>(error)

        coEvery { RewardedAd.load(adRequest) } returns sdkResult

        val result = adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = "suspend-load-placement",
        )

        assertSame(sdkResult, result)
        val failedData = slot<AdFailedToLoadData>()
        verify(exactly = 1) {
            adTracker.trackAdFailedToLoad(capture(failedData), AdCaptureMethod.ADAPTER)
        }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "suspend-load-placement",
                adUnitId = "suspend-rewarded-unit",
                mediatorErrorCode = LoadAdError.ErrorCode.NETWORK_ERROR.value,
            ),
            failedData.captured,
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingRewardedAdEventCallback()

        rewardedAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { rewardedAd.adEventCallback = eventCallback }
    }

    private class RecordingRewardedAdEventCallback : RewardedAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }

    private class RecordingRewardedLoadCallback : AdLoadCallback<RewardedAd> {
        var loadedAd: RewardedAd? = null
        var loadError: LoadAdError? = null

        override fun onAdLoaded(ad: RewardedAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }
    }
}
