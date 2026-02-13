@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingFullScreenContentCallbackBehaviorTest {

    @Test
    fun `onAdDismissedFullScreenContent delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createSubject(delegate)

        subject.onAdDismissedFullScreenContent()

        assertEquals(1, delegate.onAdDismissedCalls)
    }

    @Test
    fun `onAdFailedToShowFullScreenContent delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createSubject(delegate)

        subject.onAdFailedToShowFullScreenContent(AdError(1, "domain", "message"))

        assertEquals(1, delegate.onAdFailedToShowCalls)
    }

    @Test
    fun `onAdImpression delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createSubject(delegate)

        subject.onAdImpression()

        assertEquals(1, delegate.onAdImpressionCalls)
    }

    @Test
    fun `delegation only callbacks do not crash without delegate`() {
        val subject = createSubject(delegate = null)

        subject.onAdDismissedFullScreenContent()
        subject.onAdFailedToShowFullScreenContent(AdError(2, "domain", "message"))
        subject.onAdImpression()
    }

    private fun createSubject(delegate: FullScreenContentCallback?): TrackingFullScreenContentCallback {
        return TrackingFullScreenContentCallback(
            delegate = delegate,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "test-ad-unit",
            responseInfoProvider = { throw AssertionError("Not expected in delegation-only tests") },
        )
    }

    private class RecordingFullScreenContentCallback : FullScreenContentCallback() {
        var onAdDismissedCalls: Int = 0
        var onAdFailedToShowCalls: Int = 0
        var onAdImpressionCalls: Int = 0

        override fun onAdDismissedFullScreenContent() {
            onAdDismissedCalls++
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            onAdFailedToShowCalls++
        }

        override fun onAdImpression() {
            onAdImpressionCalls++
        }
    }
}
