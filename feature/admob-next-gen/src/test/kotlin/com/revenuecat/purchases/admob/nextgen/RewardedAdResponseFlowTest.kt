@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingRewardedAdEventCallback
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

class RewardedAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdResponseFlowContract(
        adTracker = adTracker,
        adapter = RewardedAdAdapter(),
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
    fun `response success tracks and installs event callback before forwarding`() {
        val eventCallback = RecordingRewardedEventCallback()

        contract.responseSuccess(
            eventCallback = eventCallback,
            assertDelegateInvoked = { assertTrue(eventCallback.metadataChangedCalled) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure()
    }

    private inner class RewardedAdAdapter : FullScreenAdResponseFlowAdapter<RewardedAd, RewardedAdEventCallback> {
        override val values = FullScreenAdTestValues(
            adFormat = AdFormat.REWARDED,
            adUnitId = "supplied-rewarded-unit",
            placement = "response-rewarded",
        )

        override fun createAd(
            responseInfo: ResponseInfo,
            installedCallback: CallbackHolder<RewardedAdEventCallback>,
            onCallbackInstalled: () -> Unit,
        ): RewardedAd = mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers {
                installedCallback.callback = firstArg()
                onCallbackInstalled()
            }
        }

        override fun stubLoadFromResponse(
            trackingLoadCallback: CallbackHolder<AdLoadCallback<RewardedAd>>,
        ) {
            every { RewardedAd.loadFromAdResponse("opaque-response", any()) } answers {
                trackingLoadCallback.callback = secondArg()
            }
        }

        override fun loadAndTrackFromResponse(
            loadCallback: AdLoadCallback<RewardedAd>,
            eventCallback: RewardedAdEventCallback?,
        ) {
            adTracker.loadAndTrackRewardedAdFromResponse(
                adResponse = "opaque-response",
                adUnitId = values.adUnitId,
                placement = values.placement,
                loadCallback = loadCallback,
                adEventCallback = eventCallback,
            )
        }

        override fun asTrackingCallback(
            callback: RewardedAdEventCallback,
        ): TrackingRewardedAdEventCallback? = callback as? TrackingRewardedAdEventCallback

        override fun invokeDelegateCallback(callback: RewardedAdEventCallback) {
            callback.onAdMetadataChanged()
        }
    }

    private class RecordingRewardedEventCallback : RewardedAdEventCallback {
        var metadataChangedCalled: Boolean = false

        override fun onAdMetadataChanged() {
            metadataChangedCalled = true
        }
    }
}
