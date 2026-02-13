@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.OnPaidEventListener
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerAdFlowTest {

    @Test
    fun `banner flow preserves existing listeners and forwards events`() {
        val adView = mockk<AdView>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val existingAdListener = RecordingAdListener()
        val existingPaidListener = RecordingPaidEventListener()

        every { adView.adUnitId } returns "banner-unit"
        every { adView.adListener } returns existingAdListener
        every { adView.onPaidEventListener } returns existingPaidListener

        val adListenerSlot = slot<AdListener>()
        val paidSlot = slot<OnPaidEventListener>()
        every { adView.adListener = capture(adListenerSlot) } answers {}
        every { adView.onPaidEventListener = capture(paidSlot) } answers {}

        loadAndTrackBannerAdInternal(
            adView = adView,
            adRequest = adRequest,
            placement = "home_banner",
            adListener = null,
        )

        assertTrue(adListenerSlot.captured is TrackingAdListener)
        assertNotNull(paidSlot.captured)

        verify { adView.loadAd(adRequest) }

        adListenerSlot.captured.onAdClosed()
        assertTrue(existingAdListener.closedCalled)

        val adValue = mockk<AdValue>()
        paidSlot.captured.onPaidEvent(adValue)
        assertSame(adValue, existingPaidListener.lastAdValue)
    }

    @Test
    fun `explicit onPaidEventListener takes precedence over pre-existing listener`() {
        val adView = mockk<AdView>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val preExistingPaidListener = RecordingPaidEventListener()
        val explicitPaidListener = RecordingPaidEventListener()

        every { adView.adUnitId } returns "banner-unit"
        every { adView.onPaidEventListener } returns preExistingPaidListener

        val paidSlot = slot<OnPaidEventListener>()
        every { adView.onPaidEventListener = capture(paidSlot) } answers {}
        every { adView.adListener = any() } answers {}

        loadAndTrackBannerAdInternal(
            adView = adView,
            adRequest = adRequest,
            placement = "home_banner",
            onPaidEventListener = explicitPaidListener,
        )

        assertTrue(
            "Installed listener should be a TrackingOnPaidEventListener",
            paidSlot.captured is TrackingOnPaidEventListener,
        )
        assertSame(
            "Delegate should be the explicit parameter, not the pre-existing listener",
            explicitPaidListener,
            (paidSlot.captured as TrackingOnPaidEventListener).delegate,
        )
    }

    @Test
    fun `calling loadAndTrackBannerAd twice does not double-wrap listeners`() {
        // Use vars to simulate what the AdView stores — the mock getter returns
        // whatever was last set, just like a real AdView property.
        var storedAdListener: AdListener = mockk(relaxed = true)
        var storedPaidListener: OnPaidEventListener = mockk(relaxed = true)

        val adView = mockk<AdView>(relaxed = true)
        val adRequest = mockk<AdRequest>()
        val userAdListener = RecordingAdListener()
        val userPaidListener = RecordingPaidEventListener()

        every { adView.adUnitId } returns "banner-unit"
        every { adView.adListener } answers { storedAdListener }
        every { adView.onPaidEventListener } answers { storedPaidListener }
        every { adView.adListener = any() } answers { storedAdListener = firstArg<AdListener>() }
        every { adView.onPaidEventListener = any() } answers { storedPaidListener = firstArg<OnPaidEventListener>() }

        // Pre-set the user's own listeners before the first call.
        storedAdListener = userAdListener
        storedPaidListener = userPaidListener

        // First call — wraps the user listeners with tracking wrappers.
        loadAndTrackBannerAdInternal(
            adView = adView,
            adRequest = adRequest,
            placement = "home_banner",
            adListener = null,
        )

        // Second call — should unwrap and re-wrap, NOT double-wrap.
        loadAndTrackBannerAdInternal(
            adView = adView,
            adRequest = adRequest,
            placement = "home_banner",
            adListener = null,
        )

        // After two calls the outermost adListener must be a single
        // TrackingAdListener whose delegate is the original user listener —
        // NOT another TrackingAdListener (which would mean double-wrapping).
        val finalAdListener = storedAdListener
        assertTrue(
            "Final adListener should be a TrackingAdListener",
            finalAdListener is TrackingAdListener,
        )
        assertSame(
            "TrackingAdListener.delegate should be the original user listener, not a nested wrapper",
            userAdListener,
            (finalAdListener as TrackingAdListener).delegate,
        )

        // Same structural check for the paid-event listener.
        val finalPaidListener = storedPaidListener
        assertTrue(
            "Final onPaidEventListener should be a TrackingOnPaidEventListener",
            finalPaidListener is TrackingOnPaidEventListener,
        )
        assertSame(
            "TrackingOnPaidEventListener.delegate should be the original user listener, not a nested wrapper",
            userPaidListener,
            (finalPaidListener as TrackingOnPaidEventListener).delegate,
        )
    }

    private class RecordingAdListener : AdListener() {
        var closedCalled: Boolean = false

        override fun onAdClosed() {
            closedCalled = true
        }
    }

    private class RecordingPaidEventListener : OnPaidEventListener {
        var lastAdValue: AdValue? = null

        override fun onPaidEvent(adValue: AdValue) {
            lastAdValue = adValue
        }
    }
}
