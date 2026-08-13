@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
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
class RewardedAdFlowTest {

    @Before
    fun setUp() {
        mockkObject(RewardedAd.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(RewardedAd.Companion)
    }

    @Test
    fun `rewarded success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "rewarded-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
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

        mockk<AdTracker>().loadAndTrackRewardedAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(rewardedAd)

        assertSame(rewardedAd, loadCallback.loadedAd)
        val trackingCallback = installedCallback as TrackingRewardedAdEventCallback
        trackingCallback.onAdMetadataChanged()
        assertTrue(initialEventCallback.metadataChanged)

        rewardedAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdMetadataChanged()
        assertTrue(replacementEventCallback.metadataChanged)

        rewardedAd.show(activity, "show-placement", rewardListener)
        assertSame("show-placement", trackingCallback.placement)
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

        mockk<AdTracker>().loadAndTrackRewardedAd(
            adRequest = adRequest,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
    }

    @Test
    fun `setting rewarded event callback falls back when tracking is not installed`() {
        val rewardedAd = mockk<RewardedAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingRewardedAdEventCallback()

        rewardedAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { rewardedAd.adEventCallback = eventCallback }
    }

    private class RecordingRewardedAdEventCallback : RewardedAdEventCallback {
        var metadataChanged: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChanged = true
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
