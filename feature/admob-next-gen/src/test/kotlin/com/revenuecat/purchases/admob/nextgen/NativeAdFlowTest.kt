@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.nextgen.tracking.TrackingNativeAdEventCallback
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
class NativeAdFlowTest {

    @Before
    fun setUp() {
        mockkObject(NativeAdLoader.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(NativeAdLoader.Companion)
    }

    @Test
    fun `native success installs tracking before forwarding the loaded ad`() {
        val adRequest = mockk<NativeAdRequest> {
            every { adUnitId } returns "native-unit"
        }
        val responseInfo = mockk<ResponseInfo>(relaxed = true)
        var installedCallback: NativeAdEventCallback? = null
        val nativeAd = mockk<NativeAd>(relaxed = true) {
            every { getResponseInfo() } returns responseInfo
            every { adEventCallback } answers { installedCallback }
            every { adEventCallback = any() } answers { installedCallback = firstArg() }
        }
        val loadCallback = RecordingNativeAdLoaderCallback()
        val initialEventCallback = RecordingNativeAdEventCallback()
        val replacementEventCallback = RecordingNativeAdEventCallback()
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()

        every { NativeAdLoader.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackNativeAd(
            adRequest = adRequest,
            placement = "native-placement",
            nativeAdLoaderCallback = loadCallback,
            adEventCallback = initialEventCallback,
        )
        trackingLoadCallback.captured.onNativeAdLoaded(nativeAd)

        assertSame(nativeAd, loadCallback.loadedNativeAd)
        val trackingCallback = installedCallback as TrackingNativeAdEventCallback
        trackingCallback.onAdSwipeGestureClicked()
        assertTrue(initialEventCallback.swipeClicked)

        nativeAd.setTrackingAdEventCallback(replacementEventCallback)
        trackingCallback.onAdSwipeGestureClicked()
        assertTrue(replacementEventCallback.swipeClicked)
    }

    @Test
    fun `native loader forwards non-native results failure and completion`() {
        val adRequest = mockk<NativeAdRequest> {
            every { adUnitId } returns "native-unit"
        }
        val customNativeAd = mockk<CustomNativeAd>()
        val bannerAd = mockk<BannerAd>()
        val error = mockk<LoadAdError>(relaxed = true)
        val loadCallback = RecordingNativeAdLoaderCallback()
        val trackingLoadCallback = slot<NativeAdLoaderCallback>()

        every { NativeAdLoader.load(adRequest, capture(trackingLoadCallback)) } just runs

        mockk<AdTracker>().loadAndTrackNativeAd(
            adRequest = adRequest,
            nativeAdLoaderCallback = loadCallback,
        )
        trackingLoadCallback.captured.onCustomNativeAdLoaded(customNativeAd)
        trackingLoadCallback.captured.onBannerAdLoaded(bannerAd)
        trackingLoadCallback.captured.onAdFailedToLoad(error)
        trackingLoadCallback.captured.onAdLoadingCompleted()

        assertSame(customNativeAd, loadCallback.loadedCustomNativeAd)
        assertSame(bannerAd, loadCallback.loadedBannerAd)
        assertSame(error, loadCallback.loadError)
        assertTrue(loadCallback.loadingCompleted)
    }

    @Test
    fun `setting native event callback falls back when tracking is not installed`() {
        val nativeAd = mockk<NativeAd>(relaxed = true) {
            every { adEventCallback } returns null
        }
        val eventCallback = RecordingNativeAdEventCallback()

        nativeAd.setTrackingAdEventCallback(eventCallback)

        verify(exactly = 1) { nativeAd.adEventCallback = eventCallback }
    }

    private class RecordingNativeAdEventCallback : NativeAdEventCallback {
        var swipeClicked: Boolean = false

        override fun onAdSwipeGestureClicked() {
            swipeClicked = true
        }
    }

    private class RecordingNativeAdLoaderCallback : NativeAdLoaderCallback {
        var loadedNativeAd: NativeAd? = null
        var loadedCustomNativeAd: CustomNativeAd? = null
        var loadedBannerAd: BannerAd? = null
        var loadError: LoadAdError? = null
        var loadingCompleted: Boolean = false

        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            loadedNativeAd = nativeAd
        }

        override fun onCustomNativeAdLoaded(customNativeAd: CustomNativeAd) {
            loadedCustomNativeAd = customNativeAd
        }

        override fun onBannerAdLoaded(bannerAd: BannerAd) {
            loadedBannerAd = bannerAd
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            loadError = adError
        }

        override fun onAdLoadingCompleted() {
            loadingCompleted = true
        }
    }
}
