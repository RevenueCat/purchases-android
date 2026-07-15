package com.revenuecat.purchases.integration.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * End-to-end check of the workflows paywall-components memory optimization + kill-switch fallback:
 * - With workflows on and `/v1/config` healthy, `getOfferings` returns an offering whose `paywallComponents`
 *   were NOT captured (memory saved), yet `hasPaywall` is still true (cheap presence flag).
 * - After a `/v1/config` 4xx trips the session kill switch, the offerings cache is invalidated and a refetch
 *   re-parses with remote config disabled, so `paywallComponents` are captured and decodable again for the
 *   fallback render path.
 *
 * Drives the real backend (the production test project's current offering ships `paywall_components` + `ui_config`)
 * with mocked billing; only `/v1/config` is faked, via [ForceServerErrorStrategy.fakeResponseWithoutPerformingRequest].
 */
@RunWith(AndroidJUnit4::class)
@OptIn(InternalRevenueCatAPI::class)
class ProductionWorkflowsPaywallComponentsIntegrationTest : BasePurchasesIntegrationTest() {

    override val environmentConfig get() = Constants.production

    override val dangerousSettings: DangerousSettings get() = DangerousSettings.forWorkflows()

    override var forceServerErrorsStrategy: ForceServerErrorStrategy? = RemoteConfigKillSwitchFake()

    private val remoteConfigFake get() = forceServerErrorsStrategy as RemoteConfigKillSwitchFake

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun offeringsSkipPaywallComponentsUntilRemoteConfigKillSwitch() = runBlocking<Unit> {
        confirmProductionBackendEnvironment()
        mockBillingAbstract.mockQueryProductDetails()

        // Phase 1: remote config healthy (faked 204) + workflows on => components are NOT captured, but the
        // offering still reports a paywall via the cheap presence flag.
        val phase1Current = currentOffering()
        assertThat(phase1Current.hasPaywall).isTrue()
        assertThat(phase1Current.paywallComponents).isNull()

        // Phase 2: make /v1/config return a 4xx and force a refresh => the session kill switch trips, the
        // orchestrator invalidates the in-memory offerings cache and refetches. syncAttributesAndOfferingsIfNeeded
        // runs an unconditional remote-config refresh, which now hits the faked 4xx.
        remoteConfigFake.disableRemoteConfig = true
        runCatching { Purchases.sharedInstance.awaitSyncAttributesAndOfferingsIfNeeded() }

        // Phase 3: after the kill switch, a fetch re-parses with remote config disabled, so paywall components are
        // captured and available for the fallback render path.
        val phase3Current = awaitCurrentOfferingWithComponents()
        assertThat(phase3Current.hasPaywall).isTrue()
        assertThat(phase3Current.paywallComponents).isNotNull
        // Force the lazy decode to prove the captured component tree is valid.
        assertThat(phase3Current.paywallComponents!!.data).isNotNull
    }

    private suspend fun currentOffering(): Offering =
        Purchases.sharedInstance.awaitOfferings().current ?: fail("Expected a current offering")

    private suspend fun awaitCurrentOfferingWithComponents(): Offering {
        // The disable + cache invalidation + refetch happen asynchronously, so retry until the refetched offering
        // carries the captured components (bounded, so a regression fails instead of hanging).
        repeat(MAX_POLL_ATTEMPTS) {
            val current = currentOffering()
            if (current.paywallComponents != null) return current
            delay(POLL_INTERVAL)
        }
        return fail("Expected a current offering with paywall components after the kill switch")
    }

    private class RemoteConfigKillSwitchFake : ForceServerErrorStrategy {
        @Volatile
        var disableRemoteConfig = false

        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = false

        override fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
            if (endpoint !is Endpoint.GetRemoteConfig) return null
            return if (disableRemoteConfig) {
                // A 4xx client error => SHOULD_DISABLE => session kill switch.
                fakeResult(
                    RCHTTPStatusCodes.BAD_REQUEST,
                    """{"code":7000,"message":"remote config disabled for test"}""",
                )
            } else {
                // 204 => success no-op: keeps remote config enabled without fabricating a real RC container.
                fakeResult(RCHTTPStatusCodes.NO_CONTENT, "")
            }
        }

        private fun fakeResult(responseCode: Int, payload: String) = HTTPResult(
            responseCode = responseCode,
            payload = payload,
            origin = HTTPResult.Origin.BACKEND,
            requestDate = null,
            verificationResult = VerificationResult.NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )
    }

    private companion object {
        const val MAX_POLL_ATTEMPTS = 20
        val POLL_INTERVAL: Duration = 500.milliseconds
    }
}
