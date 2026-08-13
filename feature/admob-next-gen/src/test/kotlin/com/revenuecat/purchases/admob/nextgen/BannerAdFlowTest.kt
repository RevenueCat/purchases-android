@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdEventCallback
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingBannerAdRefreshCallback
import com.revenuecat.purchases.ads.events.AdTracker
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BannerAdFlowTest {

    @Test
    fun `banner success installs tracking callbacks before forwarding loaded ad`() {
        val adView = mockk<AdView>()
        val adRequest = mockk<BannerAdRequest> {
            every { adUnitId } returns "banner-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
        }
        val loadCallback = RecordingAdLoadCallback<BannerAd>()
        val eventCallback = RecordingBannerAdEventCallback()
        val refreshCallback = RecordingBannerAdRefreshCallback()
        val trackingLoadCallback = slot<AdLoadCallback<BannerAd>>()
        val installedEventCallback = slot<BannerAdEventCallback>()
        val installedRefreshCallback = slot<BannerAdRefreshCallback>()

        every { adView.loadAd(adRequest, capture(trackingLoadCallback)) } just runs
        every { bannerAd.adEventCallback = capture(installedEventCallback) } just runs
        every { bannerAd.bannerAdRefreshCallback = capture(installedRefreshCallback) } just runs

        adView.loadAndTrackAd(
            adRequest = adRequest,
            placement = "home-banner",
            loadCallback = loadCallback,
            adEventCallback = eventCallback,
            bannerAdRefreshCallback = refreshCallback,
        )
        trackingLoadCallback.captured.onAdLoaded(bannerAd)

        assertSame(bannerAd, loadCallback.loadedAd)
        assertTrue(installedEventCallback.captured is TrackingBannerAdEventCallback)
        assertTrue(installedRefreshCallback.captured is TrackingBannerAdRefreshCallback)

        installedEventCallback.captured.onAppEvent("name", "data")
        installedRefreshCallback.captured.onAdRefreshed()

        assertTrue(eventCallback.appEventCalled)
        assertTrue(refreshCallback.refreshedCalled)
    }

    @Test
    fun `banner callbacks can be replaced without losing tracking`() {
        val adView = mockk<AdView>()
        val adRequest = mockk<BannerAdRequest> {
            every { adUnitId } returns "banner-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
        var installedEventCallback: BannerAdEventCallback? = null
        var installedRefreshCallback: BannerAdRefreshCallback? = null
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedEventCallback }
            every { adEventCallback = any() } answers { installedEventCallback = firstArg() }
            every { bannerAdRefreshCallback } answers { installedRefreshCallback }
            every { bannerAdRefreshCallback = any() } answers { installedRefreshCallback = firstArg() }
        }
        val replacementEventCallback = RecordingBannerAdEventCallback()
        val replacementRefreshCallback = RecordingBannerAdRefreshCallback()
        val trackingLoadCallback = slot<AdLoadCallback<BannerAd>>()

        every { adView.loadAd(adRequest, capture(trackingLoadCallback)) } just runs

        adView.loadAndTrackAd(adRequest = adRequest, placement = "home-banner")
        trackingLoadCallback.captured.onAdLoaded(bannerAd)

        val trackingEventCallback = installedEventCallback as TrackingBannerAdEventCallback
        val trackingRefreshCallback = installedRefreshCallback as TrackingBannerAdRefreshCallback

        bannerAd.setTrackingAdEventCallback(replacementEventCallback)
        bannerAd.setTrackingBannerAdRefreshCallback(replacementRefreshCallback)

        // The tracking wrappers are still installed, and now forward to the new delegates.
        assertSame(trackingEventCallback, installedEventCallback)
        assertSame(trackingRefreshCallback, installedRefreshCallback)

        trackingEventCallback.onAppEvent("name", "data")
        trackingRefreshCallback.onAdRefreshed()

        assertTrue(replacementEventCallback.appEventCalled)
        assertTrue(replacementRefreshCallback.refreshedCalled)
    }

    @Test
    fun `setting banner callbacks directly falls back when tracking is not installed`() {
        val eventCallback = RecordingBannerAdEventCallback()
        val refreshCallback = RecordingBannerAdRefreshCallback()
        val bannerAd = mockk<BannerAd>(relaxed = true) {
            every { adEventCallback } returns null
            every { bannerAdRefreshCallback } returns null
        }

        bannerAd.setTrackingAdEventCallback(eventCallback)
        bannerAd.setTrackingBannerAdRefreshCallback(refreshCallback)

        verify(exactly = 1) { bannerAd.adEventCallback = eventCallback }
        verify(exactly = 1) { bannerAd.bannerAdRefreshCallback = refreshCallback }
    }

    @Test
    fun `banner failure is forwarded through tracker entry point`() {
        val adView = mockk<AdView>()
        val adRequest = mockk<BannerAdRequest> {
            every { adUnitId } returns "banner-unit"
        }
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingAdLoadCallback<BannerAd>()
        val trackingLoadCallback = slot<AdLoadCallback<BannerAd>>()

        every { adView.loadAd(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackBannerAd(
            adView = adView,
            adRequest = adRequest,
            loadCallback = loadCallback,
        )
        trackingLoadCallback.captured.onAdFailedToLoad(error)

        assertSame(error, loadCallback.loadError)
        verify(exactly = 1) { adView.loadAd(adRequest, any()) }
    }

    private class RecordingBannerAdEventCallback : BannerAdEventCallback {
        var appEventCalled: Boolean = false

        override fun onAppEvent(name: String, data: String?) {
            appEventCalled = true
        }
    }

    private class RecordingBannerAdRefreshCallback : BannerAdRefreshCallback {
        var refreshedCalled: Boolean = false

        override fun onAdRefreshed() {
            refreshedCalled = true
        }
    }
}

private class RecordingAdLoadCallback<AdT> : AdLoadCallback<AdT> {
    var loadedAd: AdT? = null
    var loadError: LoadAdError? = null

    override fun onAdLoaded(ad: AdT) {
        loadedAd = ad
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        loadError = adError
    }
}
