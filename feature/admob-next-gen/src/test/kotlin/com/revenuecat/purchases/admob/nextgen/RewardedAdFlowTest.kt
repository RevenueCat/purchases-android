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

class RewardedAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val values = FullScreenAdTestValues(AdFormat.REWARDED, "rewarded-unit", "load-placement")
    private val suspendingValues = FullScreenAdTestValues(
        AdFormat.REWARDED,
        "suspend-rewarded-unit",
        "suspend-load-placement",
    )

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
        val adRequest = mockk<AdRequest> { every { adUnitId } returns values.adUnitId }
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
        val loadCallback = FullScreenRecordingAdLoadCallback<RewardedAd>()
        val initialEventCallback = RecordingRewardedAdEventCallback()
        val replacementEventCallback = RecordingRewardedAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()

        every { RewardedAd.load(adRequest, capture(trackingLoadCallback)) } just runs
        adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedAd)

        assertSame(rewardedAd, loadCallback.loadedAd)
        adTracker.assertLoadedData(slot(), values, "test-network", "response-id")

        val trackingCallback = requireNotNull(installedCallback as? TrackingRewardedAdEventCallback)
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

        rewardedAd.show(activity, placement = null, onUserEarnedRewardListener = rewardListener)

        assertNull(trackingCallback.placement)
        verify(exactly = 1) { rewardedAd.show(activity, rewardListener) }
    }

    @Test
    fun `rewarded failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns values.adUnitId }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = FullScreenRecordingAdLoadCallback<RewardedAd>()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedAd>>()

        every { RewardedAd.load(adRequest, capture(trackingLoadCallback)) } just runs
        adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = values.placement,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
        adTracker.assertFailedData(slot(), values)
    }

    @Test
    fun `suspending rewarded success tracks and installs callback before returning original result`() = runBlocking {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns suspendingValues.adUnitId }
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
            placement = suspendingValues.placement,
            adEventCallback = eventCallback,
        )

        assertSame(sdkResult, result)
        val trackingCallback = requireNotNull(installedCallback as? TrackingRewardedAdEventCallback)
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
    fun `suspending rewarded failure tracks error and returns original result`() = runBlocking {
        val adRequest = mockk<AdRequest> { every { adUnitId } returns suspendingValues.adUnitId }
        val error = mockk<LoadAdError> { every { code } returns LoadAdError.ErrorCode.NETWORK_ERROR }
        val sdkResult = AdLoadResult.Failure<RewardedAd>(error)

        coEvery { RewardedAd.load(adRequest) } returns sdkResult
        val result = adTracker.loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = suspendingValues.placement,
        )

        assertSame(sdkResult, result)
        adTracker.assertSuspendingFailedData(slot<AdFailedToLoadData>(), suspendingValues)
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
}
