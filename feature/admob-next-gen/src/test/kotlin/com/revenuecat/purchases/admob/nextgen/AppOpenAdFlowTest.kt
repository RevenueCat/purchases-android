@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingAppOpenAdEventCallback
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
class AppOpenAdFlowTest {

    @Before
    fun setUp() {
        mockkObject(AppOpenAd.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(AppOpenAd.Companion)
    }

    @Test
    fun `app open success installs tracking and supports placement and delegate updates`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "app-open-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
        var installedCallback: AppOpenAdEventCallback? = null
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val activity = mockk<Activity>()
        val loadCallback = RecordingAppOpenLoadCallback()
        val initialEventCallback = RecordingAppOpenAdEventCallback()
        val replacementEventCallback = RecordingAppOpenAdEventCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackAppOpenAd(
            adRequest = adRequest,
            placement = "load-placement",
            loadCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(appOpenAd)

        assertSame(appOpenAd, loadCallback.loadedAd)
        val trackingCallback = installedCallback as TrackingAppOpenAdEventCallback
        trackingCallback.onAdDismissedFullScreenContent()
        assertTrue(initialEventCallback.dismissed)

        appOpenAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdDismissedFullScreenContent()
        assertTrue(replacementEventCallback.dismissed)

        appOpenAd.show(activity, "show-placement")
        assertSame("show-placement", trackingCallback.placement)
        verify(exactly = 1) { appOpenAd.show(activity) }
    }

    @Test
    fun `app open failure is forwarded to load callback`() {
        val adRequest = mockk<AdRequest> {
            every { adUnitId } returns "app-open-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingAppOpenLoadCallback()
        val trackingLoadCallback = slot<AdLoadCallback<AppOpenAd>>()

        every { AppOpenAd.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackAppOpenAd(
            adRequest = adRequest,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
    }

    @Test
    fun `setting app open event callback falls back when tracking is not installed`() {
        val appOpenAd = mockk<AppOpenAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingAppOpenAdEventCallback()

        appOpenAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { appOpenAd.adEventCallback = eventCallback }
    }

    private class RecordingAppOpenAdEventCallback : AppOpenAdEventCallback {
        var dismissed: Boolean = false

        override fun onAdDismissedFullScreenContent() {
            dismissed = true
        }
    }

    private class RecordingAppOpenLoadCallback : AdLoadCallback<AppOpenAd> {
        var loadedAd: AppOpenAd? = null
        var loadError: LoadAdError? = null

        override fun onAdLoaded(ad: AppOpenAd) {
            loadedAd = ad
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }
    }
}
