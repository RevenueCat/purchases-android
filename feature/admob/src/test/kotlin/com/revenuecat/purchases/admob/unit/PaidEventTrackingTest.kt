@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.admob.tracking.TrackingOnPaidEventListener
import com.revenuecat.purchases.admob.tracking.setUpPaidEventTracking
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PaidEventTrackingTest {

    private val mockAdTracker = mockk<AdTracker>(relaxed = true)
    private val mockPurchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        every { mockPurchases.adTracker } returns mockAdTracker
        setPurchasesSingleton(mockPurchases)
    }

    @After
    fun tearDown() {
        setPurchasesSingleton(null)
    }

    @Test
    fun `setUpPaidEventTracking installs listener`() {
        var captured: OnPaidEventListener? = null

        setUpPaidEventTracking(
            setListener = { captured = it },
            adFormat = AdFormat.INTERSTITIAL,
            placementProvider = { "home_interstitial" },
            adUnitId = "test-ad-unit",
            responseInfoProvider = { mockk(relaxed = true) },
            delegate = null,
        )

        assertNotNull(captured)
    }

    @Test
    fun `TrackingOnPaidEventListener onPaidEvent calls trackAdRevenue with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "com.google.ads.mediation.admob.AdMobAdapter"
        every { responseInfo.responseId } returns "resp-123"

        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 50_000L
        every { adValue.currencyCode } returns "USD"
        every { adValue.precisionType } returns AdValue.PrecisionType.PRECISE

        val listener = TrackingOnPaidEventListener(
            delegate = null,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "home_interstitial",
            adUnitId = "ca-app-pub-123/456",
            responseInfoProvider = { responseInfo },
        )

        listener.onPaidEvent(adValue)

        val slot = slot<AdRevenueData>()
        verify(exactly = 1) { mockAdTracker.trackAdRevenue(capture(slot)) }
        assertEquals(
            AdRevenueData(
                networkName = "com.google.ads.mediation.admob.AdMobAdapter",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "home_interstitial",
                adUnitId = "ca-app-pub-123/456",
                impressionId = "resp-123",
                revenueMicros = 50_000L,
                currency = "USD",
                precision = AdRevenuePrecision.EXACT,
            ),
            slot.captured,
        )
    }

    @Test
    fun `TrackingOnPaidEventListener forwards onPaidEvent to delegate`() {
        val delegate = RecordingPaidEventListener()
        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 1L
        every { adValue.currencyCode } returns "EUR"
        every { adValue.precisionType } returns AdValue.PrecisionType.ESTIMATED

        val listener = TrackingOnPaidEventListener(
            delegate = delegate,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        listener.onPaidEvent(adValue)

        assertNotNull(delegate.lastAdValue)
        assertEquals(adValue, delegate.lastAdValue)
    }

    @Test
    fun `TrackingOnPaidEventListener with null delegate still tracks revenue`() {
        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 1L
        every { adValue.currencyCode } returns "EUR"
        every { adValue.precisionType } returns AdValue.PrecisionType.ESTIMATED

        val listener = TrackingOnPaidEventListener(
            delegate = null,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "unit",
            responseInfoProvider = { mockk(relaxed = true) },
        )

        listener.onPaidEvent(adValue)

        verify(exactly = 1) { mockAdTracker.trackAdRevenue(any()) }
    }

    @Test
    fun `TrackingOnPaidEventListener uses empty string when responseId is null`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns null
        every { responseInfo.responseId } returns null

        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 100L
        every { adValue.currencyCode } returns "JPY"
        every { adValue.precisionType } returns AdValue.PrecisionType.ESTIMATED

        val listener = TrackingOnPaidEventListener(
            delegate = null,
            adFormat = AdFormat.NATIVE,
            placement = null,
            adUnitId = "native-unit",
            responseInfoProvider = { responseInfo },
        )

        listener.onPaidEvent(adValue)

        val slot = slot<AdRevenueData>()
        verify { mockAdTracker.trackAdRevenue(capture(slot)) }
        assertEquals("", slot.captured.impressionId)
        assertEquals(null, slot.captured.networkName)
        assertEquals(null, slot.captured.placement)
    }

    @Test
    fun `setUpPaidEventTracking installed listener calls trackAdRevenue with correct data`() {
        val responseInfo = mockk<ResponseInfo>()
        every { responseInfo.mediationAdapterClassName } returns "TestNetwork"
        every { responseInfo.responseId } returns "resp-456"

        var captured: OnPaidEventListener? = null

        setUpPaidEventTracking(
            setListener = { captured = it },
            adFormat = AdFormat.REWARDED,
            placementProvider = { "rewarded_placement" },
            adUnitId = "rewarded-unit",
            responseInfoProvider = { responseInfo },
            delegate = null,
        )

        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 75_000L
        every { adValue.currencyCode } returns "GBP"
        every { adValue.precisionType } returns AdValue.PrecisionType.PUBLISHER_PROVIDED

        captured!!.onPaidEvent(adValue)

        val slot = slot<AdRevenueData>()
        verify(exactly = 1) { mockAdTracker.trackAdRevenue(capture(slot)) }
        assertEquals(
            AdRevenueData(
                networkName = "TestNetwork",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_placement",
                adUnitId = "rewarded-unit",
                impressionId = "resp-456",
                revenueMicros = 75_000L,
                currency = "GBP",
                precision = AdRevenuePrecision.PUBLISHER_DEFINED,
            ),
            slot.captured,
        )
    }

    @Test
    fun `setUpPaidEventTracking installed listener forwards to delegate`() {
        val delegate = RecordingPaidEventListener()

        var captured: OnPaidEventListener? = null

        setUpPaidEventTracking(
            setListener = { captured = it },
            adFormat = AdFormat.APP_OPEN,
            placementProvider = { "app_open" },
            adUnitId = "app-open-unit",
            responseInfoProvider = { mockk(relaxed = true) },
            delegate = delegate,
        )

        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 1L
        every { adValue.currencyCode } returns "USD"
        every { adValue.precisionType } returns AdValue.PrecisionType.ESTIMATED

        captured!!.onPaidEvent(adValue)

        assertNotNull(delegate.lastAdValue)
        assertEquals(adValue, delegate.lastAdValue)
    }

    @Test
    fun `setUpPaidEventTracking resolves placement lazily at event time`() {
        var currentPlacement: String? = "load_time"
        var captured: OnPaidEventListener? = null

        setUpPaidEventTracking(
            setListener = { captured = it },
            adFormat = AdFormat.INTERSTITIAL,
            placementProvider = { currentPlacement },
            adUnitId = "test-unit",
            responseInfoProvider = { mockk(relaxed = true) },
            delegate = null,
        )

        currentPlacement = "show_time"

        val adValue = mockk<AdValue>()
        every { adValue.valueMicros } returns 1L
        every { adValue.currencyCode } returns "USD"
        every { adValue.precisionType } returns AdValue.PrecisionType.ESTIMATED

        captured!!.onPaidEvent(adValue)

        val slot = slot<AdRevenueData>()
        verify { mockAdTracker.trackAdRevenue(capture(slot)) }
        assertEquals("show_time", slot.captured.placement)
    }

    private class RecordingPaidEventListener : OnPaidEventListener {
        var lastAdValue: AdValue? = null

        override fun onPaidEvent(adValue: AdValue) {
            lastAdValue = adValue
        }
    }

    companion object {
        private val backingField = Purchases::class.java.getDeclaredField("backingFieldSharedInstance").apply {
            isAccessible = true
        }

        fun setPurchasesSingleton(instance: Purchases?) {
            backingField.set(null, instance)
        }
    }
}
