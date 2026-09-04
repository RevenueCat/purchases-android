@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingInterstitialAdEventCallback
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

class InterstitialAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdResponseFlowContract(
        adTracker = adTracker,
        adapter = InterstitialAdAdapter(),
    )

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(InterstitialAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(InterstitialAd.Companion)
    }

    @Test
    fun `response success tracks and installs event callback before forwarding`() {
        val eventCallback = RecordingInterstitialEventCallback()

        contract.responseSuccess(
            eventCallback = eventCallback,
            assertDelegateInvoked = { assertTrue(eventCallback.appEventCalled) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure()
    }

    private inner class InterstitialAdAdapter :
        FullScreenAdResponseFlowAdapter<InterstitialAd, InterstitialAdEventCallback> {
        override val values = FullScreenAdTestValues(
            adFormat = AdFormat.INTERSTITIAL,
            adUnitId = "supplied-interstitial-unit",
            placement = "response-interstitial",
        )

        override fun createAd(
            responseInfo: ResponseInfo,
            installedCallback: CallbackHolder<InterstitialAdEventCallback>,
            onCallbackInstalled: () -> Unit,
        ): InterstitialAd = mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers {
                installedCallback.callback = firstArg()
                onCallbackInstalled()
            }
        }

        override fun stubLoadFromResponse(
            trackingLoadCallback: CallbackHolder<AdLoadCallback<InterstitialAd>>,
        ) {
            every { InterstitialAd.loadFromAdResponse("opaque-response", any()) } answers {
                trackingLoadCallback.callback = secondArg()
            }
        }

        override fun loadAndTrackFromResponse(
            loadCallback: AdLoadCallback<InterstitialAd>,
            eventCallback: InterstitialAdEventCallback?,
        ) {
            adTracker.loadAndTrackInterstitialAdFromResponse(
                adResponse = "opaque-response",
                adUnitId = values.adUnitId,
                placement = values.placement,
                loadCallback = loadCallback,
                adEventCallback = eventCallback,
            )
        }

        override fun asTrackingCallback(
            callback: InterstitialAdEventCallback,
        ): TrackingInterstitialAdEventCallback? = callback as? TrackingInterstitialAdEventCallback

        override fun invokeDelegateCallback(callback: InterstitialAdEventCallback) {
            callback.onAppEvent("name", "data")
        }
    }

    private class RecordingInterstitialEventCallback : InterstitialAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }
}
