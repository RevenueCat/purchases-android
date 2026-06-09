@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.tracking

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
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

class TrackingAdListenerBehaviorTest {

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
    fun `onAdClosed delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createDelegationSubject(delegate)

        subject.onAdClosed()

        assertEquals(1, delegate.onAdClosedCalls)
    }

    @Test
    fun `onAdOpened delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createDelegationSubject(delegate)

        subject.onAdOpened()

        assertEquals(1, delegate.onAdOpenedCalls)
    }

    @Test
    fun `onAdSwipeGestureClicked delegates to wrapped listener`() {
        val delegate = RecordingAdListener()
        val subject = createDelegationSubject(delegate)

        subject.onAdSwipeGestureClicked()

        assertEquals(1, delegate.onAdSwipeGestureClickedCalls)
    }

    @Test
    fun `delegation only callbacks do not crash without delegate`() {
        val subject = createDelegationSubject(delegate = null)

        subject.onAdClosed()
        subject.onAdOpened()
        subject.onAdSwipeGestureClicked()
    }

    // endregion

    // region Tracking tests — onAdLoaded

    @Test
    fun `onAdLoaded calls trackAdLoaded with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "com.google.ads.mediation.admob.AdMobAdapter"
        every { responseInfo.responseId } returns "resp-loaded-123"

        val delegate = RecordingAdListener()
        val subject = TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home_banner",
            adUnitId = "banner-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdLoaded()

        val slot = slot<AdLoadedData>()
        verify(exactly = 1) { mockAdTracker.trackAdLoaded(capture(slot)) }
        assertEquals(
            AdLoadedData(
                networkName = "com.google.ads.mediation.admob.AdMobAdapter",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home_banner",
                adUnitId = "banner-unit",
                impressionId = "resp-loaded-123",
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdLoadedCalls)
    }

    @Test
    fun `onAdLoaded with trackAdLoaded false skips tracking but still delegates`() {
        val delegate = RecordingAdListener()
        val subject = TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.NATIVE,
            placement = "native_feed",
            adUnitId = "native-unit",
            responseInfoProvider = { mockk(relaxed = true) },
            trackAdLoaded = false,
        )

        subject.onAdLoaded()

        verify(exactly = 0) { mockAdTracker.trackAdLoaded(any()) }
        assertEquals(1, delegate.onAdLoadedCalls)
    }

    // endregion

    // region Tracking tests — onAdImpression

    @Test
    fun `onAdImpression calls trackAdDisplayed with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "TestNetwork"
        every { responseInfo.responseId } returns "resp-impression-456"

        val delegate = RecordingAdListener()
        val subject = TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home_banner",
            adUnitId = "banner-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdImpression()

        val slot = slot<AdDisplayedData>()
        verify(exactly = 1) { mockAdTracker.trackAdDisplayed(capture(slot)) }
        assertEquals(
            AdDisplayedData(
                networkName = "TestNetwork",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home_banner",
                adUnitId = "banner-unit",
                impressionId = "resp-impression-456",
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdImpressionCalls)
    }

    // endregion

    // region Tracking tests — onAdClicked

    @Test
    fun `onAdClicked calls trackAdOpened with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "ClickNetwork"
        every { responseInfo.responseId } returns "resp-clicked-789"

        val delegate = RecordingAdListener()
        val subject = TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home_banner",
            adUnitId = "banner-unit",
            responseInfoProvider = { responseInfo },
        )

        subject.onAdClicked()

        val slot = slot<AdOpenedData>()
        verify(exactly = 1) { mockAdTracker.trackAdOpened(capture(slot)) }
        assertEquals(
            AdOpenedData(
                networkName = "ClickNetwork",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home_banner",
                adUnitId = "banner-unit",
                impressionId = "resp-clicked-789",
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdClickedCalls)
    }

    // endregion

    // region Tracking tests — onAdFailedToLoad

    @Test
    fun `onAdFailedToLoad calls trackAdFailedToLoad with correct data`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns 3

        val delegate = RecordingAdListener()
        val subject = TrackingAdListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "home_banner",
            adUnitId = "banner-unit",
            responseInfoProvider = { null },
        )

        subject.onAdFailedToLoad(error)

        val slot = slot<AdFailedToLoadData>()
        verify(exactly = 1) { mockAdTracker.trackAdFailedToLoad(capture(slot)) }
        assertEquals(
            AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "home_banner",
                adUnitId = "banner-unit",
                mediatorErrorCode = 3,
            ),
            slot.captured,
        )
        assertEquals(1, delegate.onAdFailedToLoadCalls)
    }

    // endregion

    // region Null delegate still tracks

    @Test
    fun `onAdLoaded with null delegate still tracks`() {
        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdLoaded()

        verify(exactly = 1) { mockAdTracker.trackAdLoaded(any()) }
    }

    @Test
    fun `onAdImpression with null delegate still tracks`() {
        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdImpression()

        verify(exactly = 1) { mockAdTracker.trackAdDisplayed(any()) }
    }

    @Test
    fun `onAdClicked with null delegate still tracks`() {
        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdClicked()

        verify(exactly = 1) { mockAdTracker.trackAdOpened(any()) }
    }

    @Test
    fun `onAdFailedToLoad with null delegate still tracks`() {
        val error = mockk<LoadAdError>()
        every { error.code } returns 1

        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { null },
        )

        subject.onAdFailedToLoad(error)

        verify(exactly = 1) { mockAdTracker.trackAdFailedToLoad(any()) }
    }

    // endregion

    // region Negative tests

    @Test
    fun `onAdClosed does not trigger any tracking`() {
        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdClosed()

        verify(exactly = 0) { mockAdTracker.trackAdLoaded(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdDisplayed(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdOpened(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdRevenue(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdFailedToLoad(any()) }
    }

    @Test
    fun `onAdOpened does not trigger any tracking`() {
        val subject = TrackingAdListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        subject.onAdOpened()

        verify(exactly = 0) { mockAdTracker.trackAdLoaded(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdDisplayed(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdOpened(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdRevenue(any()) }
        verify(exactly = 0) { mockAdTracker.trackAdFailedToLoad(any()) }
    }

    // endregion

    private fun createDelegationSubject(delegate: AdListener?): TrackingAdListener {
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
        var onAdLoadedCalls: Int = 0
        var onAdImpressionCalls: Int = 0
        var onAdClickedCalls: Int = 0
        var onAdFailedToLoadCalls: Int = 0

        override fun onAdClosed() {
            onAdClosedCalls++
        }

        override fun onAdOpened() {
            onAdOpenedCalls++
        }

        override fun onAdSwipeGestureClicked() {
            onAdSwipeGestureClickedCalls++
        }

        override fun onAdLoaded() {
            onAdLoadedCalls++
        }

        override fun onAdImpression() {
            onAdImpressionCalls++
        }

        override fun onAdClicked() {
            onAdClickedCalls++
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            onAdFailedToLoadCalls++
        }
    }
}
