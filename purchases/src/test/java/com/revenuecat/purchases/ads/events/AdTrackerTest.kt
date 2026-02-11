package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.common.events.EventsManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
public class AdTrackerTest {

    private lateinit var eventsManager: EventsManager
    private lateinit var adTracker: AdTracker

    @Before
    public fun setUp() {
        eventsManager = mockk()
        adTracker = AdTracker(eventsManager)
    }

    @Test
    fun `trackAdDisplayed tracks displayed event`() {
        val eventSlot = slot<AdEvent.Displayed>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdDisplayed(
            data = AdDisplayedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.BANNER,
                placement = "banner_home",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.Displayed>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.BANNER)
        assertThat(eventSlot.captured.placement).isEqualTo("banner_home")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-123")
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.DISPLAYED)
    }

    @Test
    fun `trackAdDisplayed with null placement tracks displayed event`() {
        val eventSlot = slot<AdEvent.Displayed>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdDisplayed(
            data = AdDisplayedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = null,
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.Displayed>()) }
        assertThat(eventSlot.captured.placement).isNull()
    }

    @Test
    fun `trackAdOpened tracks opened event`() {
        val eventSlot = slot<AdEvent.Open>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdOpened(
            data = AdOpenedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.NATIVE,
                placement = "interstitial_level_complete",
                adUnitId = "ca-app-pub-789012",
                impressionId = "impression-456",
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.Open>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.NATIVE)
        assertThat(eventSlot.captured.placement).isEqualTo("interstitial_level_complete")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-789012")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-456")
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.OPENED)
    }

    @Test
    fun `trackAdRevenue tracks revenue event`() {
        val eventSlot = slot<AdEvent.Revenue>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRevenue(
            data = AdRevenueData(
                networkName = "AppLovin",
                mediatorName = AdMediatorName.APP_LOVIN,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ad-unit-999",
                impressionId = "impression-789",
                revenueMicros = 1500000,
                currency = "USD",
                precision = AdRevenuePrecision.EXACT,
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.Revenue>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("AppLovin")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.APP_LOVIN)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.REWARDED)
        assertThat(eventSlot.captured.placement).isEqualTo("rewarded_video")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ad-unit-999")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-789")
        assertThat(eventSlot.captured.revenueMicros).isEqualTo(1500000)
        assertThat(eventSlot.captured.currency).isEqualTo("USD")
        assertThat(eventSlot.captured.precision).isEqualTo(AdRevenuePrecision.EXACT)
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.REVENUE)
    }

    @Test
    fun `trackAdRevenue with different precision values`() {
        val eventSlot = slot<AdEvent.Revenue>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRevenue(
            data = AdRevenueData(
                networkName = "Network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.MREC,
                placement = "placement",
                adUnitId = "ad-unit",
                impressionId = "impression",
                revenueMicros = 1000000,
                currency = "EUR",
                precision = AdRevenuePrecision.ESTIMATED,
            ),
        )

        assertThat(eventSlot.captured.precision).isEqualTo(AdRevenuePrecision.ESTIMATED)
    }

    @Test
    fun `trackAdLoaded tracks loaded event`() {
        val eventSlot = slot<AdEvent.Loaded>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdLoaded(
            data = AdLoadedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.INTERSTITIAL,
                placement = "interstitial_level_complete",
                adUnitId = "ca-app-pub-789012",
                impressionId = "impression-456",
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.Loaded>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.INTERSTITIAL)
        assertThat(eventSlot.captured.placement).isEqualTo("interstitial_level_complete")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-789012")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-456")
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.LOADED)
    }

    @Test
    fun `trackAdFailedToLoad tracks failed to load event`() {
        val eventSlot = slot<AdEvent.FailedToLoad>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdFailedToLoad(
            data = AdFailedToLoadData(
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.APP_OPEN,
                placement = "interstitial_level_complete",
                adUnitId = "ca-app-pub-789012",
                mediatorErrorCode = 123,
            ),
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.FailedToLoad>()) }

        assertThat(eventSlot.captured.networkName).isNull()
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.APP_OPEN)
        assertThat(eventSlot.captured.placement).isEqualTo("interstitial_level_complete")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-789012")
        assertThat(eventSlot.captured.impressionId).isNull()
        assertThat(eventSlot.captured.mediatorErrorCode).isEqualTo(123)
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.FAILED_TO_LOAD)
    }
}
