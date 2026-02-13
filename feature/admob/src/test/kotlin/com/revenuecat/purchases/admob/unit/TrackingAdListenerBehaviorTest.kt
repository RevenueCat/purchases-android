@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdListener
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingAdListenerBehaviorTest {

    @Test
    fun `onAdClosed delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createSubject(delegate)

        subject.onAdClosed()

        assertEquals(1, delegate.onAdClosedCalls)
    }

    @Test
    fun `onAdOpened delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createSubject(delegate)

        subject.onAdOpened()

        assertEquals(1, delegate.onAdOpenedCalls)
    }

    @Test
    fun `onAdSwipeGestureClicked delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createSubject(delegate)

        subject.onAdSwipeGestureClicked()

        assertEquals(1, delegate.onAdSwipeGestureClickedCalls)
    }

    @Test
    fun `delegation only callbacks do not crash without delegate`() {
        val subject = createSubject(delegate = null)

        subject.onAdClosed()
        subject.onAdOpened()
        subject.onAdSwipeGestureClicked()
    }

    private fun createSubject(delegate: AdListener?): TrackingAdListener {
        return TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home_banner",
            adUnitId = "test-ad-unit",
            responseInfoProvider = { null },
        )
    }

    private class RecordingAdListener : AdListener() {
        var onAdClosedCalls: Int = 0
        var onAdOpenedCalls: Int = 0
        var onAdSwipeGestureClickedCalls: Int = 0

        override fun onAdClosed() {
            onAdClosedCalls++
        }

        override fun onAdOpened() {
            onAdOpenedCalls++
        }

        override fun onAdSwipeGestureClicked() {
            onAdSwipeGestureClickedCalls++
        }
    }
}
