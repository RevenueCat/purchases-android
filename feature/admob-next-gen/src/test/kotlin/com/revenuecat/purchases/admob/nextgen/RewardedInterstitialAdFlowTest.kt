@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
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

class RewardedInterstitialAdFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdFlowContract<RewardedInterstitialAd, RewardedInterstitialAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(
            AdFormat.REWARDED_INTERSTITIAL,
            "rewarded-interstitial-unit",
            "load-placement",
        ),
        suspendingValues = FullScreenAdTestValues(
            AdFormat.REWARDED_INTERSTITIAL,
            "suspend-rewarded-interstitial-unit",
            "suspend-load-placement",
        ),
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
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val initialEventCallback = RecordingRewardedInterstitialAdEventCallback()
        val replacementEventCallback = RecordingRewardedInterstitialAdEventCallback()

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
                every { RewardedInterstitialAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback, eventCallback ->
                adTracker.loadAndTrackRewardedInterstitialAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                    adEventCallback = eventCallback,
                )
            },
            asTrackingCallback = { it as? TrackingRewardedInterstitialAdEventCallback },
            invokeDelegateCallback = { it.onAdMetadataChanged() },
            assertInitialDelegateInvoked = { assertTrue(initialEventCallback.metadataChangedCalled) },
            setTrackingEventCallback = { ad, eventCallback -> ad.setTrackingAdEventCallback(eventCallback) },
            assertReplacementDelegateInvoked = { assertTrue(replacementEventCallback.metadataChangedCalled) },
            show = { ad, activity, placement -> ad.show(activity, placement, rewardListener) },
            verifyShow = { ad, activity -> verify(exactly = 1) { ad.show(activity, rewardListener) } },
        )
    }

    @Test
    fun `show with null placement clears the load-time placement`() {
        val installedCallback = CallbackHolder<RewardedInterstitialAdEventCallback>()
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { adEventCallback } answers { installedCallback.callback }
            every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
        }
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-interstitial-unit"
        }
        val trackingLoadCallback = CallbackHolder<AdLoadCallback<RewardedInterstitialAd>>()
        every { RewardedInterstitialAd.load(adRequest, any()) } answers {
            trackingLoadCallback.callback = secondArg()
        }
        adTracker.loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = FullScreenRecordingAdLoadCallback(),
        )
        trackingLoadCallback.requireCallback().onAdLoaded(rewardedInterstitialAd)

        val trackingCallback = requireNotNull(
            installedCallback.requireCallback() as? TrackingRewardedInterstitialAdEventCallback,
        )
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        assertShowClearsPlacement(
            trackingCallback = trackingCallback,
            show = { rewardedInterstitialAd.show(activity, placement = null, rewardListener) },
            verifyShow = { verify(exactly = 1) { rewardedInterstitialAd.show(activity, rewardListener) } },
        )
    }

    @Test
    fun `rewarded interstitial failure is forwarded to load callback`() {
        contract.callbackFailure(
            stubLoad = { adRequest, trackingLoadCallback ->
                every { RewardedInterstitialAd.load(adRequest, any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrack = { adRequest, loadCallback ->
                adTracker.loadAndTrackRewardedInterstitialAd(
                    adRequest = adRequest,
                    placement = "load-placement",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    @Test
    fun `suspending rewarded interstitial success tracks and installs callback before returning original result`() =
        runBlocking {
            val eventCallback = RecordingRewardedInterstitialAdEventCallback()

            contract.suspendingSuccess(
                createAd = { responseInfo, installedCallback ->
                    mockk(relaxed = true) {
                        every { getResponseInfo() } returns responseInfo
                        every { adEventCallback } answers { installedCallback.callback }
                        every { adEventCallback = any() } answers { installedCallback.callback = firstArg() }
                    }
                },
                eventCallback = eventCallback,
                stubLoad = { adRequest, sdkResult ->
                    coEvery { RewardedInterstitialAd.load(adRequest) } returns sdkResult
                },
                loadAndTrack = { adRequest, delegate ->
                    adTracker.loadAndTrackRewardedInterstitialAd(
                        adRequest = adRequest,
                        placement = "suspend-load-placement",
                        adEventCallback = delegate,
                    )
                },
                asTrackingCallback = { it as? TrackingRewardedInterstitialAdEventCallback },
                invokeDelegateCallback = { it.onAdMetadataChanged() },
                assertDelegateInvoked = { assertTrue(eventCallback.metadataChangedCalled) },
            )
        }

    @Test
    fun `suspending rewarded interstitial failure tracks error and returns original result`() = runBlocking {
        contract.suspendingFailure(
            stubLoad = { adRequest, sdkResult ->
                coEvery { RewardedInterstitialAd.load(adRequest) } returns sdkResult
            },
            loadAndTrack = { adRequest ->
                adTracker.loadAndTrackRewardedInterstitialAd(
                    adRequest = adRequest,
                    placement = "suspend-load-placement",
                )
            },
        )
    }

    @Test
    fun `setting event callback directly falls back when tracking is not installed`() {
        val eventCallback = RecordingRewardedInterstitialAdEventCallback()

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

    private class RecordingRewardedInterstitialAdEventCallback : RewardedInterstitialAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
