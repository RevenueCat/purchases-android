package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.ads.events.types.AdDisplayedData
import com.revenuecat.purchases.ads.events.types.AdFailedToLoadData
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdLoadedData
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdOpenedData
import com.revenuecat.purchases.ads.events.types.AdRevenueData
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.ads.events.types.AdRewardEarnedUnverifiedData
import com.revenuecat.purchases.ads.events.types.AdRewardFailedToVerifyData
import com.revenuecat.purchases.ads.events.types.AdRewardFailureReason
import com.revenuecat.purchases.ads.events.types.AdRewardGrantedData
import com.revenuecat.purchases.ads.events.types.AdRewardVerifiedData
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
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
class AdTrackerTest {

    private lateinit var eventsManager: EventsManager
    private lateinit var adTracker: AdTracker

    @Before
    fun setUp() {
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
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `internal trackAdDisplayed overload stamps the given capture method`() {
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
            captureMethod = AdCaptureMethod.ADAPTER,
        )

        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.ADAPTER)
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
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
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
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRevenue with different precision values`() {
        val eventSlot = slot<AdEvent.Revenue>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRevenue(
            data = AdRevenueData(
                networkName = "Network",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.OTHER,
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
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
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
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRewardEarnedUnverified tracks reward earned unverified event`() {
        val eventSlot = slot<AdEvent.RewardEarnedUnverified>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardEarnedUnverified(
            data = AdRewardEarnedUnverifiedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                rewardVerificationEnabled = true,
            ),
            captureMethod = AdCaptureMethod.MANUAL,
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.RewardEarnedUnverified>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.REWARDED)
        assertThat(eventSlot.captured.placement).isEqualTo("rewarded_video")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-123")
        assertThat(eventSlot.captured.rewardVerificationEnabled).isTrue()
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.REWARD_EARNED_UNVERIFIED)
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRewardEarnedUnverified stamps the given capture method`() {
        val eventSlot = slot<AdEvent.RewardEarnedUnverified>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardEarnedUnverified(
            data = AdRewardEarnedUnverifiedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                rewardVerificationEnabled = true,
            ),
            captureMethod = AdCaptureMethod.ADAPTER,
        )

        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.ADAPTER)
    }

    @Test
    fun `trackAdRewardVerified tracks reward verified event`() {
        val eventSlot = slot<AdEvent.RewardVerified>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardVerified(
            data = AdRewardVerifiedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
            ),
            captureMethod = AdCaptureMethod.MANUAL,
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.RewardVerified>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.REWARDED)
        assertThat(eventSlot.captured.placement).isEqualTo("rewarded_video")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-123")
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.REWARD_VERIFIED)
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRewardVerified stamps the given capture method`() {
        val eventSlot = slot<AdEvent.RewardVerified>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardVerified(
            data = AdRewardVerifiedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
            ),
            captureMethod = AdCaptureMethod.ADAPTER,
        )

        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.ADAPTER)
    }

    @Test
    fun `trackAdRewardGranted tracks reward granted event`() {
        val eventSlot = slot<AdEvent.RewardGranted>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardGranted(
            data = AdRewardGrantedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                reward = VerifiedReward.VirtualCurrency(code = "GLD", amount = 100),
            ),
            captureMethod = AdCaptureMethod.MANUAL,
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.RewardGranted>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.REWARDED)
        assertThat(eventSlot.captured.placement).isEqualTo("rewarded_video")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-123")
        assertThat(eventSlot.captured.reward).isEqualTo(VerifiedReward.VirtualCurrency(code = "GLD", amount = 100))
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.REWARD_GRANTED)
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRewardGranted stamps the given capture method`() {
        val eventSlot = slot<AdEvent.RewardGranted>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardGranted(
            data = AdRewardGrantedData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                reward = VerifiedReward.Entitlement(identifier = "premium", expiresAt = Date(0)),
            ),
            captureMethod = AdCaptureMethod.ADAPTER,
        )

        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.ADAPTER)
    }

    @Test
    fun `trackAdRewardFailedToVerify tracks reward failed to verify event`() {
        val eventSlot = slot<AdEvent.RewardFailedToVerify>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardFailedToVerify(
            data = AdRewardFailedToVerifyData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                failureReason = AdRewardFailureReason.Timeout,
            ),
            captureMethod = AdCaptureMethod.MANUAL,
        )

        verify(exactly = 1) { eventsManager.track(any<AdEvent.RewardFailedToVerify>()) }

        assertThat(eventSlot.captured.networkName).isEqualTo("Google AdMob")
        assertThat(eventSlot.captured.mediatorName).isEqualTo(AdMediatorName.AD_MOB)
        assertThat(eventSlot.captured.adFormat).isEqualTo(AdFormat.REWARDED)
        assertThat(eventSlot.captured.placement).isEqualTo("rewarded_video")
        assertThat(eventSlot.captured.adUnitId).isEqualTo("ca-app-pub-123456")
        assertThat(eventSlot.captured.impressionId).isEqualTo("impression-123")
        assertThat(eventSlot.captured.failureReason).isEqualTo(AdRewardFailureReason.Timeout)
        assertThat(eventSlot.captured.type).isEqualTo(AdEventType.REWARD_FAILED_TO_VERIFY)
        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.MANUAL)
    }

    @Test
    fun `trackAdRewardFailedToVerify stamps the given capture method`() {
        val eventSlot = slot<AdEvent.RewardFailedToVerify>()
        every { eventsManager.track(capture(eventSlot)) } just Runs

        adTracker.trackAdRewardFailedToVerify(
            data = AdRewardFailedToVerifyData(
                networkName = "Google AdMob",
                mediatorName = AdMediatorName.AD_MOB,
                adFormat = AdFormat.REWARDED,
                placement = "rewarded_video",
                adUnitId = "ca-app-pub-123456",
                impressionId = "impression-123",
                failureReason = AdRewardFailureReason.BackendError("no_reward_rule"),
            ),
            captureMethod = AdCaptureMethod.ADAPTER,
        )

        assertThat(eventSlot.captured.captureMethod).isEqualTo(AdCaptureMethod.ADAPTER)
    }
}
