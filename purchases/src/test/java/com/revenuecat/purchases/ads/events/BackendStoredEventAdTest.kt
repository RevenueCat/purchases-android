@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BackendStoredEventAdTest {

    private val appUserID = "test-user-123"
    private val appSessionID = "session-456"

    @Test
    fun `AdEvent Displayed converts to BackendStoredEvent Ad correctly`() {
        val displayedEvent = AdEvent.Displayed(
            id = "event-id-123",
            timestamp = 1234567890L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "banner_home",
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        val storedEvent = displayedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.id).isEqualTo("event-id-123")
        assertThat(adStoredEvent.event.version).isEqualTo(BackendEvent.AD_EVENT_SCHEMA_VERSION)
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_displayed")
        assertThat(adStoredEvent.event.timestamp).isEqualTo(1234567890L)
        assertThat(adStoredEvent.event.networkName).isEqualTo("Google AdMob")
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("AdMob")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("banner")
        assertThat(adStoredEvent.event.placement).isEqualTo("banner_home")
        assertThat(adStoredEvent.event.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-123")
        assertThat(adStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(adStoredEvent.event.appSessionID).isEqualTo(appSessionID)
        assertThat(adStoredEvent.event.revenueMicros).isNull()
        assertThat(adStoredEvent.event.currency).isNull()
        assertThat(adStoredEvent.event.precision).isNull()
    }

    @Test
    fun `AdEvent Displayed with null placement converts correctly`() {
        val displayedEvent = AdEvent.Displayed(
            id = "event-id-123",
            timestamp = 1234567890L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.INTERSTITIAL,
            placement = null,
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        val storedEvent = displayedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.placement).isNull()
        assertThat(adStoredEvent.event.adFormat).isEqualTo("interstitial")
    }

    @Test
    fun `AdEvent Open converts to BackendStoredEvent Ad correctly`() {
        val openEvent = AdEvent.Open(
            id = "event-id-456",
            timestamp = 9876543210L,
            networkName = "AppLovin",
            mediatorName = AdMediatorName.APP_LOVIN,
            adFormat = AdFormat.NATIVE,
            placement = "interstitial_level",
            adUnitId = "ad-unit-789",
            impressionId = "impression-456"
        )

        val storedEvent = openEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.id).isEqualTo("event-id-456")
        assertThat(adStoredEvent.event.version).isEqualTo(BackendEvent.AD_EVENT_SCHEMA_VERSION)
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_opened")
        assertThat(adStoredEvent.event.timestamp).isEqualTo(9876543210L)
        assertThat(adStoredEvent.event.networkName).isEqualTo("AppLovin")
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("AppLovin")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("native")
        assertThat(adStoredEvent.event.placement).isEqualTo("interstitial_level")
        assertThat(adStoredEvent.event.adUnitId).isEqualTo("ad-unit-789")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-456")
        assertThat(adStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(adStoredEvent.event.appSessionID).isEqualTo(appSessionID)
        assertThat(adStoredEvent.event.revenueMicros).isNull()
        assertThat(adStoredEvent.event.currency).isNull()
        assertThat(adStoredEvent.event.precision).isNull()
    }

    @Test
    fun `AdEvent Revenue converts to BackendStoredEvent Ad correctly`() {
        val revenueEvent = AdEvent.Revenue(
            id = "event-id-789",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            revenueMicros = 1500000,
            currency = "USD",
            precision = AdRevenuePrecision.EXACT
        )

        val storedEvent = revenueEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.id).isEqualTo("event-id-789")
        assertThat(adStoredEvent.event.version).isEqualTo(BackendEvent.AD_EVENT_SCHEMA_VERSION)
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_revenue")
        assertThat(adStoredEvent.event.timestamp).isEqualTo(1111111111L)
        assertThat(adStoredEvent.event.networkName).isEqualTo("Google AdMob")
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("AdMob")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("rewarded")
        assertThat(adStoredEvent.event.placement).isEqualTo("rewarded_video")
        assertThat(adStoredEvent.event.adUnitId).isEqualTo("ad-unit-999")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-789")
        assertThat(adStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(adStoredEvent.event.appSessionID).isEqualTo(appSessionID)
        assertThat(adStoredEvent.event.revenueMicros).isEqualTo(1500000)
        assertThat(adStoredEvent.event.currency).isEqualTo("USD")
        assertThat(adStoredEvent.event.precision).isEqualTo("exact")
    }

    @Test
    fun `AdEvent Revenue with different precision values converts correctly`() {
        val estimatedEvent = AdEvent.Revenue(
            id = "event-id-1",
            timestamp = 1111111111L,
            networkName = "Network",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.MREC,
            placement = "placement",
            adUnitId = "ad-unit",
            impressionId = "impression",
            revenueMicros = 1000000,
            currency = "EUR",
            precision = AdRevenuePrecision.ESTIMATED
        )

        val storedEvent = estimatedEvent.toBackendStoredEvent(appUserID, appSessionID)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.precision).isEqualTo("estimated")

        val publisherDefinedEvent = AdEvent.Revenue(
            id = "event-id-2",
            timestamp = 1111111111L,
            networkName = "Network",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED_INTERSTITIAL,
            placement = "placement",
            adUnitId = "ad-unit",
            impressionId = "impression",
            revenueMicros = 1000000,
            currency = "EUR",
            precision = AdRevenuePrecision.PUBLISHER_DEFINED
        )

        val storedEvent2 = publisherDefinedEvent.toBackendStoredEvent(appUserID, appSessionID)
        val adStoredEvent2 = storedEvent2 as BackendStoredEvent.Ad
        assertThat(adStoredEvent2.event.precision).isEqualTo("publisher_defined")

        val unknownEvent = AdEvent.Revenue(
            id = "event-id-3",
            timestamp = 1111111111L,
            networkName = "Network",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.APP_OPEN,
            placement = "placement",
            adUnitId = "ad-unit",
            impressionId = "impression",
            revenueMicros = 1000000,
            currency = "EUR",
            precision = AdRevenuePrecision.UNKNOWN
        )

        val storedEvent3 = unknownEvent.toBackendStoredEvent(appUserID, appSessionID)
        val adStoredEvent3 = storedEvent3 as BackendStoredEvent.Ad
        assertThat(adStoredEvent3.event.precision).isEqualTo("unknown")
    }

    @Test
    fun `AdEvent with custom mediator name converts correctly`() {
        val customMediator = AdMediatorName.fromString("CustomMediator")
        val event = AdEvent.Displayed(
            id = "event-id-custom",
            timestamp = 1234567890L,
            networkName = "Custom Network",
            mediatorName = customMediator,
            adFormat = AdFormat.OTHER,
            placement = "custom_placement",
            adUnitId = "custom-ad-unit",
            impressionId = "custom-impression"
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("CustomMediator")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("other")
    }

    @Test
    fun `AdEvent Loaded converts correctly`() {
        val revenueEvent = AdEvent.Loaded(
            id = "event-id-789",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
        )

        val storedEvent = revenueEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.id).isEqualTo("event-id-789")
        assertThat(adStoredEvent.event.version).isEqualTo(BackendEvent.AD_EVENT_SCHEMA_VERSION)
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_loaded")
        assertThat(adStoredEvent.event.timestamp).isEqualTo(1111111111L)
        assertThat(adStoredEvent.event.networkName).isEqualTo("Google AdMob")
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("AdMob")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("interstitial")
        assertThat(adStoredEvent.event.placement).isEqualTo("rewarded_video")
        assertThat(adStoredEvent.event.adUnitId).isEqualTo("ad-unit-999")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-789")
        assertThat(adStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(adStoredEvent.event.appSessionID).isEqualTo(appSessionID)
    }

    @Test
    fun `AdEvent FailedToLoad converts correctly`() {
        val failedToLoadEvent = AdEvent.FailedToLoad(
            id = "event-id-789",
            timestamp = 1111111111L,
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            mediatorErrorCode = 123,
        )

        val storedEvent = failedToLoadEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.id).isEqualTo("event-id-789")
        assertThat(adStoredEvent.event.version).isEqualTo(BackendEvent.AD_EVENT_SCHEMA_VERSION)
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_failed_to_load")
        assertThat(adStoredEvent.event.timestamp).isEqualTo(1111111111L)
        assertThat(adStoredEvent.event.networkName).isNull()
        assertThat(adStoredEvent.event.mediatorName).isEqualTo("AdMob")
        assertThat(adStoredEvent.event.adFormat).isEqualTo("banner")
        assertThat(adStoredEvent.event.placement).isEqualTo("rewarded_video")
        assertThat(adStoredEvent.event.adUnitId).isEqualTo("ad-unit-999")
        assertThat(adStoredEvent.event.mediatorErrorCode).isEqualTo(123L)
        assertThat(adStoredEvent.event.impressionId).isNull()
        assertThat(adStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(adStoredEvent.event.appSessionID).isEqualTo(appSessionID)
    }
}
