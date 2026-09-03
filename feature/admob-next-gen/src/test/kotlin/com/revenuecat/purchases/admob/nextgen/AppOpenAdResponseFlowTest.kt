@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
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
    private val contract = FullScreenAdResponseFlowContract<AppOpenAd, AppOpenAdEventCallback>(
        adTracker = adTracker,
        values = FullScreenAdTestValues(
            adFormat = AdFormat.APP_OPEN,
            adUnitId = "supplied-app-open-unit",
            placement = "response-app-open",
        ),
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
                every { AppOpenAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback, delegate ->
                adTracker.loadAndTrackAppOpenAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-app-open-unit",
                    placement = "response-app-open",
                    loadCallback = loadCallback,
                    adEventCallback = delegate,
                )
            },
            asTrackingCallback = { it as? TrackingAppOpenAdEventCallback },
            invokeDelegateCallback = { it.onAdDismissedFullScreenContent() },
            assertDelegateInvoked = { assertTrue(eventCallback.dismissed) },
        )
    }

    @Test
    fun `response failure uses supplied ad unit and placement before forwarding`() {
        contract.responseFailure(
            stubLoadFromResponse = { trackingLoadCallback ->
                every { AppOpenAd.loadFromAdResponse("opaque-response", any()) } answers {
                    trackingLoadCallback.callback = secondArg()
                }
            },
            loadAndTrackFromResponse = { loadCallback ->
                adTracker.loadAndTrackAppOpenAdFromResponse(
                    adResponse = "opaque-response",
                    adUnitId = "supplied-app-open-unit",
                    placement = "response-app-open",
                    loadCallback = loadCallback,
                )
            },
        )
    }

    private class RecordingAppOpenEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }
}
