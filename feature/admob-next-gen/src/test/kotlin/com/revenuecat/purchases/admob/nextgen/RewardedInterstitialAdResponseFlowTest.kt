@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedInterstitialAdEventCallback
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RewardedInterstitialAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract =
        FullScreenAdResponseFlowContract<RewardedInterstitialAd, RewardedInterstitialAdEventCallback>(
            adTracker = adTracker,
            values = FullScreenAdTestValues(
                adFormat = AdFormat.REWARDED_INTERSTITIAL,
                adUnitId = "supplied-rewarded-interstitial-unit",
                placement = "response-rewarded-interstitial",
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
    fun `response success tracks and installs event callback before forwarding`() {
        val eventCallback = RecordingRewardedInterstitialEventCallback()

        contract.responseSuccess(
            createAd = { responseInfo, installedCallback, onCallbackInstalled ->
                mockk(relaxed = true) {
                    every { getResponseInfo() } returns responseInfo
                    every { adEventCallback = any() } answers {
                        installedCallback.callback = firstArg()
                        onCallbackInstalled()
                    }
                }
            },
            eventCallback = eventCallback,
            stubLoadFromResponse = { trackingLoadCallback ->
                every { RewardedInterstitialAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback, delegate ->
                adTracker.loadAndTrackRewardedInterstitialAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-rewarded-interstitial-unit",
                    placement = "response-rewarded-interstitial",
                    loadCallback = loadCallback,
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingRewardedInterstitialAdEventCallback },
            invokeDelegateCallback = { it.onAdMetadataChanged() },
            assertDelegateInvoked = { assertTrue(eventCallback.metadataChangedCalled) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure(
            stubLoadFromResponse = { trackingLoadCallback ->
                every { RewardedInterstitialAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback ->
                adTracker.loadAndTrackRewardedInterstitialAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-rewarded-interstitial-unit",
                    placement = "response-rewarded-interstitial",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    private class RecordingRewardedInterstitialEventCallback : RewardedInterstitialAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
