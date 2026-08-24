@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.ads.events.types.AdRewardFailureReason
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import java.util.Date
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
            impressionId = "impression-123",
            captureMethod = AdCaptureMethod.ADAPTER,
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
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("adapter")
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
            impressionId = "impression-123",
            captureMethod = AdCaptureMethod.MANUAL,
        )

        val storedEvent = displayedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.placement).isNull()
        assertThat(adStoredEvent.event.adFormat).isEqualTo("interstitial")
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("manual")
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
            impressionId = "impression-456",
            captureMethod = AdCaptureMethod.MANUAL,
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
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("manual")
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
            captureMethod = AdCaptureMethod.ADAPTER,
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
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("adapter")
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
            adFormat = AdFormat.OTHER,
            placement = "placement",
            adUnitId = "ad-unit",
            impressionId = "impression",
            captureMethod = AdCaptureMethod.MANUAL,
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
            captureMethod = AdCaptureMethod.MANUAL,
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
            captureMethod = AdCaptureMethod.MANUAL,
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
            impressionId = "custom-impression",
            captureMethod = AdCaptureMethod.MANUAL,
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
            captureMethod = AdCaptureMethod.ADAPTER,
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
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("adapter")
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
            captureMethod = AdCaptureMethod.ADAPTER,
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
        assertThat(adStoredEvent.event.captureMethod).isEqualTo("adapter")
    }

    @Test
    fun `AdEvent RewardEarnedUnverified converts to BackendStoredEvent Ad correctly`() {
        val earnedEvent = AdEvent.RewardEarnedUnverified(
            id = "event-id-reward-earned",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
            rewardVerificationEnabled = true,
        )

        val storedEvent = earnedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_reward_sdk_earned")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-789")
        assertThat(adStoredEvent.event.rewardVerificationEnabled).isTrue()
        assertThat(adStoredEvent.event.rewardType).isNull()
        assertThat(adStoredEvent.event.rewardFailureReason).isNull()
    }

    @Test
    fun `AdEvent RewardVerified converts to BackendStoredEvent Ad correctly`() {
        val verifiedEvent = AdEvent.RewardVerified(
            id = "event-id-reward-verified",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
        )

        val storedEvent = verifiedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_reward_sdk_verified")
        assertThat(adStoredEvent.event.impressionId).isEqualTo("impression-789")
        assertThat(adStoredEvent.event.rewardVerificationEnabled).isNull()
        assertThat(adStoredEvent.event.rewardType).isNull()
        assertThat(adStoredEvent.event.rewardFailureReason).isNull()
    }

    @Test
    fun `AdEvent RewardGranted with virtual currency converts to BackendStoredEvent Ad correctly`() {
        val grantedEvent = AdEvent.RewardGranted(
            id = "event-id-reward-granted",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
            reward = VerifiedReward.VirtualCurrency(code = "GLD", amount = 100),
        )

        val storedEvent = grantedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_reward_sdk_granted")
        assertThat(adStoredEvent.event.rewardType).isEqualTo("virtual_currency")
        assertThat(adStoredEvent.event.rewardVirtualCurrencyCode).isEqualTo("GLD")
        assertThat(adStoredEvent.event.rewardVirtualCurrencyAmount).isEqualTo(100)
        assertThat(adStoredEvent.event.rewardEntitlementId).isNull()
    }

    @Test
    fun `AdEvent RewardGranted with entitlement converts to BackendStoredEvent Ad correctly`() {
        val grantedEvent = AdEvent.RewardGranted(
            id = "event-id-reward-granted-entitlement",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
            reward = VerifiedReward.Entitlement(identifier = "premium", expiresAt = Date(0)),
        )

        val storedEvent = grantedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.rewardType).isEqualTo("entitlement")
        assertThat(adStoredEvent.event.rewardEntitlementId).isEqualTo("premium")
        assertThat(adStoredEvent.event.rewardVirtualCurrencyCode).isNull()
        assertThat(adStoredEvent.event.rewardVirtualCurrencyAmount).isNull()
    }

    @Test
    fun `AdEvent RewardGranted with unsupported reward converts to BackendStoredEvent Ad correctly`() {
        val grantedEvent = AdEvent.RewardGranted(
            id = "event-id-reward-granted-unsupported",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
            reward = VerifiedReward.UnsupportedReward,
        )

        val storedEvent = grantedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.rewardType).isEqualTo("unsupported_reward")
        assertThat(adStoredEvent.event.rewardEntitlementId).isNull()
        assertThat(adStoredEvent.event.rewardVirtualCurrencyCode).isNull()
        assertThat(adStoredEvent.event.rewardVirtualCurrencyAmount).isNull()
    }

    @Test
    fun `AdEvent RewardFailedToVerify converts to BackendStoredEvent Ad correctly`() {
        val failedEvent = AdEvent.RewardFailedToVerify(
            id = "event-id-reward-failed",
            timestamp = 1111111111L,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            captureMethod = AdCaptureMethod.MANUAL,
            failureReason = AdRewardFailureReason.BackendError("no_reward_rule"),
        )

        val storedEvent = failedEvent.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.Ad::class.java)
        val adStoredEvent = storedEvent as BackendStoredEvent.Ad
        assertThat(adStoredEvent.event.type).isEqualTo("rc_ads_ad_reward_sdk_failed_to_verify")
        assertThat(adStoredEvent.event.rewardFailureReason).isEqualTo("no_reward_rule")
        assertThat(adStoredEvent.event.rewardType).isNull()
    }
}
