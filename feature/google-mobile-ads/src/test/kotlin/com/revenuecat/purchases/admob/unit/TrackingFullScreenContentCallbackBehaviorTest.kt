@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackingFullScreenContentCallbackBehaviorTest {

    private val mockAdTracker = mockk<AdTracker>(relaxed = true)
    private val mockPurchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { mockPurchases.adTracker } returns mockAdTracker
        PaidEventTrackingTest.setPurchasesSingleton(mockPurchases)
    }

    @After
    fun tearDown() {
        PaidEventTrackingTest.setPurchasesSingleton(null)
    }

    // region Delegation-only tests

    @Test
    fun `onAdDismissedFullScreenContent delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createDelegationSubject(delegate)

        subject.onAdDismissedFullScreenContent()

        assertEquals(1, delegate.onAdDismissedCalls)
    }

    @Test
    fun `onAdFailedToShowFullScreenContent delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createDelegationSubject(delegate)

        subject.onAdFailedToShowFullScreenContent(AdError(1, "domain", "message"))

        assertEquals(1, delegate.onAdFailedToShowCalls)
    }

    @Test
    fun `onAdImpression delegates to wrapped callback`() {
        val delegate = RecordingFullScreenContentCallback()
        val subject = createDelegationSubject(delegate)

        subject.onAdImpression()

        assertEquals(1, delegate.onAdImpressionCalls)
    }

    @Test
    fun `delegation only callbacks do not crash without delegate`() {
        val subject = createDelegationSubject(delegate = null)

        subject.onAdDismissedFullScreenContent()
        subject.onAdFailedToShowFullScreenContent(AdError(2, "domain", "message"))
        subject.onAdImpression()
    }

    // endregion

    // region Tracking tests

    @Test
    fun `onAdShowedFullScreenContent calls trackAdDisplayed with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "com.google.ads.mediation.admob.AdMobAdapter"
        every { responseInfo.responseId } returns "resp-displayed-123"

        val delegate = RecordingFullScreenContentCallback()
        val subject = TrackingFullScreenContentCallback(
            delegate = delegate,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "ca-app-pub-123/456",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdShowedFullScreenContent()

        val slot = slot<AdDisplayedData>()
        verify(exactly = 1) { mockAdTracker.trackAdDisplayed(capture(slot)) }
        assertEquals(
            AdDisplayedData(
                networkName = "com.google.ads.mediation.admob.AdMobAdapter",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "home_interstitial",
                adUnitId = "ca-app-pub-123/456",
                impressionId = "resp-displayed-123",
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdShowedCalls)
    }

    @Test
    fun `onAdClicked calls trackAdOpened with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "TestNetwork"
        every { responseInfo.responseId } returns "resp-clicked-456"

        val delegate = RecordingFullScreenContentCallback()
        val subject = TrackingFullScreenContentCallback(
            delegate = delegate,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_placement",
            adUnitId = "rewarded-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdClicked()

        val slot = slot<AdOpenedData>()
        verify(exactly = 1) { mockAdTracker.trackAdOpened(capture(slot)) }
        assertEquals(
            AdOpenedData(
                networkName = "TestNetwork",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_placement",
                adUnitId = "rewarded-unit",
                impressionId = "resp-clicked-456",
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdClickedCalls)
    }

    @Test
    fun `onAdShowedFullScreenContent with null delegate still tracks`() {
        val responseInfo = mockk<ResponseInfo>(relaxed = true)

        val subject = TrackingFullScreenContentCallback(
            delegate = null,
            adFormat = AdFormat.APP_OPEN,
            placement = "app_open",
            adUnitId = "app-open-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdShowedFullScreenContent()

        verify(exactly = 1) { mockAdTracker.trackAdDisplayed(any()) }
    }

    @Test
    fun `onAdClicked with null delegate still tracks`() {
        val responseInfo = mockk<ResponseInfo>(relaxed = true)

        val subject = TrackingFullScreenContentCallback(
            delegate = null,
            adFormat = AdFormat.APP_OPEN,
            placement = "app_open",
            adUnitId = "app-open-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdClicked()

        verify(exactly = 1) { mockAdTracker.trackAdOpened(any()) }
    }

    @Test
    fun `onAdDismissedFullScreenContent does not trigger any tracking`() {
        val subject = TrackingFullScreenContentCallback(
            delegate = null,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "test-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdDismissedFullScreenContent()

        verify(exactly = 0) { mockAdTracker.trackAdDisplayed(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdOpened(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdRevenue(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdLoaded(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdFailedToLoad(any()) }
    }

    @Test
    fun `onAdFailedToShowFullScreenContent does not trigger any tracking`() {
        val subject = TrackingFullScreenContentCallback(
            delegate = null,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "test-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdFailedToShowFullScreenContent(AdError(3, "domain", "message"))

        verify(exactly = 0) { mockAdTracker.trackAdDisplayed(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdOpened(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdRevenue(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdLoaded(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdFailedToLoad(any()) }
    }

    // endregion

    private fun createDelegationSubject(
        delegate: FullScreenContentCallback?,
    ): TrackingFullScreenContentCallback {
        return TrackingFullScreenContentCallback(
            delegate = delegate,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "test-ad-unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )
    }

    private class RecordingFullScreenContentCallback : FullScreenContentCallback() {
        var onAdDismissedCalls: Int = 0
        var onAdFailedToShowCalls: Int = 0
        var onAdImpressionCalls: Int = 0
        var onAdShowedCalls: Int = 0
        var onAdClickedCalls: Int = 0

        override fun onAdDismissedFullScreenContent() {
            onAdDismissedCalls++
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            onAdFailedToShowCalls++
        }

        override fun onAdImpression() {
            onAdImpressionCalls++
        }

        override fun onAdShowedFullScreenContent() {
            onAdShowedCalls++
        }

        override fun onAdClicked() {
            onAdClickedCalls++
        }
    }
}
