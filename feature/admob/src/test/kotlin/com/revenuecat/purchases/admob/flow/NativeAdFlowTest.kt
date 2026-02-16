@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeAdFlowTest {

    @Before
    fun setUp() {
        mockkConstructor(AdLoader.Builder::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(AdLoader.Builder::class)
    }

    @Test
    fun `forNativeAdWithTracking wires listeners and forwards loaded impression click paid and failure`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val adRequest = mockk<AdRequest>()
        val adLoader = mockk<AdLoader>(relaxed = true)
        val nativeAd = mockk<NativeAd>(relaxed = true)
        val responseInfo = mockk<ResponseInfo>()
        every { nativeAd.responseInfo } returns responseInfo

        val delegateAdListener = RecordingAdListener()
        val delegatePaid = RecordingPaidEventListener()
        var loadedByLambda: NativeAd? = null

        val nativeLoadedSlot = slot<NativeAd.OnNativeAdLoadedListener>()
        val adListenerSlot = slot<AdListener>()
        val paidSlot = slot<OnPaidEventListener>()

        every { anyConstructed<AdLoader.Builder>().forNativeAd(capture(nativeLoadedSlot)) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().withAdListener(capture(adListenerSlot)) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().withNativeAdOptions(any<NativeAdOptions>()) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().build() } returns adLoader
        every { nativeAd.setOnPaidEventListener(capture(paidSlot)) } answers {}

        val builder = AdLoader.Builder(context, "native-unit")
            .forNativeAdWithTracking(
                adUnitId = "native-unit",
                placement = "native",
                adListener = delegateAdListener,
                onPaidEventListener = delegatePaid,
            ) { loadedByLambda = it }

        val returnedLoader = builder.build()
        assertSame(adLoader, returnedLoader)

        returnedLoader.loadAd(adRequest)
        verify { adLoader.loadAd(adRequest) }

        nativeLoadedSlot.captured.onNativeAdLoaded(nativeAd)
        assertSame(nativeAd, loadedByLambda)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, delegatePaid.lastAdValue)

        adListenerSlot.captured.onAdImpression()
        adListenerSlot.captured.onAdClicked()
        adListenerSlot.captured.onAdFailedToLoad(mockk<LoadAdError>())
        assertTrue(delegateAdListener.impressionCalled)
        assertTrue(delegateAdListener.clickedCalled)
        assertTrue(delegateAdListener.failedCalled)
    }

    @Test
    fun `loadAndTrackNativeAd delegates to forNativeAdWithTracking and loads`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val adRequest = mockk<AdRequest>()
        val adLoader = mockk<AdLoader>(relaxed = true)
        val nativeAd = mockk<NativeAd>(relaxed = true)
        val responseInfo = mockk<ResponseInfo>()
        every { nativeAd.responseInfo } returns responseInfo

        val delegateAdListener = RecordingAdListener()
        val delegatePaid = RecordingPaidEventListener()
        var loadedByLambda: NativeAd? = null

        val nativeLoadedSlot = slot<NativeAd.OnNativeAdLoadedListener>()
        val adListenerSlot = slot<AdListener>()
        val paidSlot = slot<OnPaidEventListener>()

        every { anyConstructed<AdLoader.Builder>().forNativeAd(capture(nativeLoadedSlot)) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().withAdListener(capture(adListenerSlot)) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().withNativeAdOptions(any<NativeAdOptions>()) } answers { self as AdLoader.Builder }
        every { anyConstructed<AdLoader.Builder>().build() } returns adLoader
        every { nativeAd.setOnPaidEventListener(capture(paidSlot)) } answers {}

        val returnedLoader = RCAdMob.loadAndTrackNativeAd(
            context = context,
            adUnitId = "native-unit",
            adRequest = adRequest,
            placement = "native",
            nativeAdOptions = null,
            adListener = delegateAdListener,
            onPaidEventListener = delegatePaid,
            onAdLoaded = { loadedByLambda = it },
        )

        assertSame(adLoader, returnedLoader)
        verify { adLoader.loadAd(adRequest) }

        nativeLoadedSlot.captured.onNativeAdLoaded(nativeAd)
        assertSame(nativeAd, loadedByLambda)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, delegatePaid.lastAdValue)

        adListenerSlot.captured.onAdImpression()
        adListenerSlot.captured.onAdClicked()
        adListenerSlot.captured.onAdFailedToLoad(mockk<LoadAdError>())
        assertTrue(delegateAdListener.impressionCalled)
        assertTrue(delegateAdListener.clickedCalled)
        assertTrue(delegateAdListener.failedCalled)
    }

    private class RecordingPaidEventListener : OnPaidEventListener {
        var lastAdValue: AdValue? = null

        override fun onPaidEvent(adValue: AdValue) {
            lastAdValue = adValue
        }
    }

    private class RecordingAdListener : AdListener() {
        var impressionCalled: Boolean = false
        var clickedCalled: Boolean = false
        var failedCalled: Boolean = false

        override fun onAdImpression() {
            impressionCalled = true
        }

        override fun onAdClicked() {
            clickedCalled = true
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            failedCalled = true
        }
    }
}
