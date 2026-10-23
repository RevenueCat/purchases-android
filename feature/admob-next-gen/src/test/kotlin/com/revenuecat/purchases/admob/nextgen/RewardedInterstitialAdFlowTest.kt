@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
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

class RewardedInterstitialAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val values = FullScreenAdTestValues(
        AdFormat.REWARDED_INTERSTITIAL,
        "rewarded-interstitial-unit",
        "load-placement",
    )
    private val suspendingValues = FullScreenAdTestValues(
        AdFormat.REWARDED_INTERSTITIAL,
        "suspend-rewarded-interstitial-unit",
        "suspend-load-placement",
    )

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(RewardedInterstitialAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(RewardedInterstitialAd.Companion)
    }

    @Test
    fun `rewarded interstitial success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns values.adUnitId }
        val responseInfo = mockk<ResponseInfo>(relaxed = true) {
            every { adapterClassName } returns "test-network"
            every { responseId } returns "response-id"
        }
        var installedCallback: RewardedInterstitialAdEventCallback? = null
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val loadCallback = FullScreenRecordingAdLoadCallback<RewardedInterstitialAd>()
        val initialEventCallback = RecordingRewardedInterstitialAdEventCallback()
        val replacementEventCallback = RecordingRewardedInterstitialAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedInterstitialAd>>()

        every { RewardedInterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs
        adTracker.loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedInterstitialAd)

        assertSame(rewardedInterstitialAd, loadCallback.loadedAd)
        adTracker.assertLoadedData(slot(), values, "test-network", "response-id")

        val trackingCallback = requireNotNull(
            installedCallback as? TrackingRewardedInterstitialAdEventCallback,
        )
        trackingCallback.onAdMetadataChanged()
        assertTrue(initialEventCallback.metadataChangedCalled)

        rewardedInterstitialAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdMetadataChanged()
        assertTrue(replacementEventCallback.metadataChangedCalled)

        rewardedInterstitialAd.show(activity, "show-placement", rewardListener)
        assertEquals("show-placement", trackingCallback.placement)
        verify(exactly = 1) { rewardedInterstitialAd.show(activity, rewardListener) }
    }

    @Test
    fun `show with null placement clears the load-time placement`() {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns values.adUnitId }
        var installedCallback: RewardedInterstitialAdEventCallback? = null
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedInterstitialAd>>()

        every { RewardedInterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs
        adTracker.loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = FullScreenRecordingAdLoadCallback(),
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedInterstitialAd)

        val trackingCallback = requireNotNull(
            installedCallback as? TrackingRewardedInterstitialAdEventCallback,
        )
        assertEquals("load-placement", trackingCallback.placement)

        rewardedInterstitialAd.show(activity, placement = null, rewardListener)

        assertNull(trackingCallback.placement)
        verify(exactly = 1) { rewardedInterstitialAd.show(activity, rewardListener) }
    }

    @Test
    fun `rewarded interstitial failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns values.adUnitId }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = FullScreenRecordingAdLoadCallback<RewardedInterstitialAd>()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedInterstitialAd>>()

        every { RewardedInterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs
        adTracker.loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
        adTracker.assertFailedData(slot(), values)
    }

    @Test
    fun `suspending rewarded interstitial success tracks and installs callback before returning original result`() =
        runBlocking {
            val adRequest = mockk<AdRequest> { every { adUnitId } returns suspendingValues.adUnitId }
            val responseInfo = mockk<ResponseInfo>(relaxed = true) {
                every { adapterClassName } returns "suspend-test-network"
                every { responseId } returns "suspend-response-id"
            }
            var installedCallback: RewardedInterstitialAdEventCallback? = null
            val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
                every { getResponseInfo() } returns responseInfo
                every { adEventCallback } answers { installedCallback }
                every { adEventCallback = any() } answers { installedCallback = firstArg() }
            }
            val eventCallback = RecordingRewardedInterstitialAdEventCallback()
            val sdkResult = AdLoadResult.Success(rewardedInterstitialAd)

            coEvery { RewardedInterstitialAd.load(adRequest) } returns sdkResult
            val result = adTracker.loadAndTrackRewardedInterstitialAd(
                adRequest = adRequest,
                placement = suspendingValues.placement,
                adEventCallback = eventCallback,
            )

            assertSame(sdkResult, result)
            val trackingCallback = requireNotNull(
                installedCallback as? TrackingRewardedInterstitialAdEventCallback,
            )
            trackingCallback.onAdMetadataChanged()
            assertTrue(eventCallback.metadataChangedCalled)
            adTracker.assertLoadedData(
                slot<AdLoadedData>(),
                suspendingValues,
                "suspend-test-network",
                "suspend-response-id",
            )
        }

    @Test
    fun `suspending rewarded interstitial failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns suspendingValues.adUnitId }
        val error = mockk<LoadAdError> { every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR }
        val sdkResult = AdLoadResult.Failure<RewardedInterstitialAd>(error)

        coEvery { RewardedInterstitialAd.load(adRequest) } returns sdkResult
        val result = adTracker.loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = suspendingValues.placement,
        )

        assertSame(sdkResult, result)
        adTracker.assertSuspendingFailedData(slot<AdFailedToLoadData>(), suspendingValues)
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingRewardedInterstitialAdEventCallback()

        rewardedInterstitialAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { rewardedInterstitialAd.adEventCallback = eventCallback }
    }

    private class RecordingRewardedInterstitialAdEventCallback : RewardedInterstitialAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
