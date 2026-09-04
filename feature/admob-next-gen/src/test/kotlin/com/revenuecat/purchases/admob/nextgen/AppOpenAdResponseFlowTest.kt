@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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

class AppOpenAdResponseFlowTest {

    private val adTracker = mockk<AdTracker>(relaxed = true)
    private val purchases = mockk<Purchases>(relaxed = true)
    private val contract = FullScreenAdResponseFlowContract(
        adTracker = adTracker,
        adapter = AppOpenAdAdapter(),
    )

    @Before
    fun setUp() {
        every { purchases.adTracker } returns adTracker
        mockkObject(AppOpenAd.Companion)
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
        unmockkObject(AppOpenAd.Companion)
    }

    @Test
    fun `response success tracks and installs event callback before forwarding`() {
        val eventCallback = RecordingAppOpenEventCallback()

        contract.responseSuccess(
            eventCallback = eventCallback,
            assertDelegateInvoked = { assertTrue(eventCallback.dismissed) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure()
    }

    private inner class AppOpenAdAdapter : FullScreenAdResponseFlowAdapter<AppOpenAd, AppOpenAdEventCallback> {
        override val values = FullScreenAdTestValues(
            adFormat = AdFormat.APP_OPEN,
            adUnitId = "supplied-app-open-unit",
            placement = "response-app-open",
        )

        override fun createAd(
            responseInfo: ResponseInfo,
            installedCallback: CallbackHolder<AppOpenAdEventCallback>,
            onCallbackInstalled: () -> Unit,
        ): AppOpenAd = mockk(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback = any() } answers {
                installedCallback.callback = firstArg()
                onCallbackInstalled()
            }
        }

        override fun stubLoadFromResponse(
            trackingLoadCallback: CallbackHolder<AdLoadCallback<AppOpenAd>>,
        ) {
            every { AppOpenAd.loadFromAdResponse("opaque-response", any()) } answers {
                trackingLoadCallback.callback = secondArg()
            }
        }

        override fun loadAndTrackFromResponse(
            loadCallback: AdLoadCallback<AppOpenAd>,
            eventCallback: AppOpenAdEventCallback?,
        ) {
            adTracker.loadAndTrackAppOpenAdFromResponse(
                adResponse = "opaque-response",
                adUnitId = values.adUnitId,
                placement = values.placement,
                loadCallback = loadCallback,
                adEventCallback = eventCallback,
            )
        }

        override fun asTrackingCallback(
            callback: AppOpenAdEventCallback,
        ): TrackingAppOpenAdEventCallback? = callback as? TrackingAppOpenAdEventCallback

        override fun invokeDelegateCallback(callback: AppOpenAdEventCallback) {
            callback.onAdDismissedFullScreenContent()
        }
    }

    private class RecordingAppOpenEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }
}
