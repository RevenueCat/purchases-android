@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RewardedInterstitialAdFlowTest {

    @Before
    fun setUp() {
        mockkObject(RewardedInterstitialAd.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(RewardedInterstitialAd.Companion)
    }

    @Test
    fun `rewarded interstitial success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-interstitial-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
        var installedCallback: RewardedInterstitialAdEventCallback? = null
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val rewardListener = mockk<OnUserEarnedRewardListener>()
        val loadCallback = RecordingRewardedInterstitialLoadCallback()
        val initialEventCallback = RecordingRewardedInterstitialAdEventCallback()
        val replacementEventCallback = RecordingRewardedInterstitialAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedInterstitialAd>>()

        every { RewardedInterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedInterstitialAd)

        assertSame(rewardedInterstitialAd, loadCallback.loadedAd)
        val trackingCallback = installedCallback as TrackingRewardedInterstitialAdEventCallback
        trackingCallback.onAdMetadataChanged()
        assertTrue(initialEventCallback.metadataChanged)

        rewardedInterstitialAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdMetadataChanged()
        assertTrue(replacementEventCallback.metadataChanged)

        rewardedInterstitialAd.show(activity, "show-placement", rewardListener)
        assertSame("show-placement", trackingCallback.placement)
        verify(exactly = 1) { rewardedInterstitialAd.show(activity, rewardListener) }
    }

    @Test
    fun `rewarded interstitial failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-interstitial-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingRewardedInterstitialLoadCallback()
        val trackingLoadCallback = slot<AdLoadCallback<RewardedInterstitialAd>>()

        every { RewardedInterstitialAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackRewardedInterstitialAd(
            adRequest = adRequest,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
    }

    @Test
    fun `setting rewarded interstitial event callback falls back without tracking`() {
        val rewardedInterstitialAd = mockk<RewardedInterstitialAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingRewardedInterstitialAdEventCallback()

        rewardedInterstitialAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { rewardedInterstitialAd.adEventCallback = eventCallback }
    }

    private class RecordingRewardedInterstitialAdEventCallback : RewardedInterstitialAdEventCallback {
        var metadataChanged: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChanged = true
        }
    }

    private class RecordingRewardedInterstitialLoadCallback : AdLoadCallback<RewardedInterstitialAd> {
        var loadedAd: RewardedInterstitialAd? = null
        var loadError: LoadAdError? = null

        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }
    }
}
