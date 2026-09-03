@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RewardedAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdFlowContract<RewardedAd, RewardedAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(AdFormat.REWARDED, "rewarded-unit", "load-placement"),
        suspendingValues = FullScreenAdTestValues(
            AdFormat.REWARDED,
            "suspend-rewarded-unit",
            "suspend-load-placement",
        ),
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
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val initialEventCallback = RecordingRewardedAdEventCallback()
        val replacementEventCallback = RecordingRewardedAdEventCallback()

        contract.callbackSuccess(
            createAd = { responseInfo, installedCallback ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback } answers { installedCallback.callback }
                    every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                }
            },
            initialEventCallback = initialEventCallback,
            replacementEventCallback = replacementEventCallback,
            stubLoad = { adRequest, trackingLoadCallback ->
                every { RewardedAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback, eventCallback ->
                adTracker.loadAndTrackRewardedAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                    adEventCallback = eventCallback,
                )
            },
            asTrackingCallback = { it as? TrackingRewardedAdEventCallback },
            invokeDelegateCallback = { it.onAdMetadataChanged() },
            assertInitialDelegateInvoked = { assertTrue(initialEventCallback.metadataChangedCalled) },
            setTrackingEventCallback = { ad, eventCallback -> ad.setTrackingAdEventCallback(eventCallback) },
            assertReplacementDelegateInvoked = { assertTrue(replacementEventCallback.metadataChangedCalled) },
            show = { ad, activity, placement -> ad.show(activity, placement, rewardListener) },
            verifyShow = { ad, activity -> verify(exactly = 1) { ad.show(activity, rewardListener) } },
        )
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

        assertShowClearsPlacement(
            trackingCallback = trackingCallback,
            show = {
                rewardedAd.show(
                    activity = activity,
                    placement = null,
                    onUserEarnedRewardListener = rewardListener,
                )
            },
            verifyShow = { verify(exactly = 1) { rewardedAd.show(activity, rewardListener) } },
        )
    }

    @Test
    fun `rewarded failure is forwarded to load callback`() {
        contract.callbackFailure(
            stubLoad = { adRequest, trackingLoadCallback ->
                every { RewardedAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback ->
                adTracker.loadAndTrackRewardedAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    @Test
    fun `suspending rewarded success tracks and installs callback before returning original result`() = runBlocking {
        val eventCallback = RecordingRewardedAdEventCallback()

        contract.suspendingSuccess(
            createAd = { responseInfo, installedCallback ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback } answers { installedCallback.callback }
                    every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                }
            },
            eventCallback = eventCallback,
            stubLoad = { adRequest, sdkResult -> coEvery { RewardedAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest, delegate ->
                adTracker.loadAndTrackRewardedAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingRewardedAdEventCallback },
            invokeDelegateCallback = { it.onAdMetadataChanged() },
            assertDelegateInvoked = { assertTrue(eventCallback.metadataChangedCalled) },
        )
    }

    @Test
    fun `suspending rewarded failure tracks error and returns original result`() = runBlocking {
        contract.suspendingFailure(
            stubLoad = { adRequest, sdkResult -> coEvery { RewardedAd.load(adRequest) } returns sdkResult },
            loadAndTrack = { adRequest ->
                adTracker.loadAndTrackRewardedAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                )
            },
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val eventCallback = RecordingRewardedAdEventCallback()

        contract.eventCallbackFallback(
            createAd = {
                mockk(relaxed = true) {
                    every { adEventCallback } returns null
                }
            },
            eventCallback = eventCallback,
            setTrackingEventCallback = { ad, callback -> ad.setTrackingAdEventCallback(callback) },
            verifyEventCallbackInstalled = { ad, callback ->
                verify(exactly = 1) { ad.adEventCallback = callback }
            },
        )
    }

    private class RecordingRewardedAdEventCallback : RewardedAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
