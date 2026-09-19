package com.revenuecat.purchases.integration.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

/**
 * End-to-end check of the workflows paywall-components memory optimization: with workflows on and `/v1/config`
 * available, `getOfferings` returns an offering whose `paywallComponents` were NOT captured (memory saved), yet
 * `hasPaywall` is still true (cheap presence flag) — components are served from the config endpoint instead.
 *
 * Drives the real backend (the production test project's current offering ships `paywall_components` + `ui_config`)
 * with mocked billing; only `/v1/config` is faked, via [ForceServerErrorStrategy.fakeResponseWithoutPerformingRequest].
 */
@RunWith(AndroidJUnit4::class)
class ProductionWorkflowsPaywallComponentsIntegrationTest : BasePurchasesIntegrationTest() {

    override val environmentConfig get() = Constants.production

    override var forceServerErrorsStrategy: ForceServerErrorStrategy? = RemoteConfigNoOpFake()

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    @Test
    fun offeringsSkipPaywallComponentsWhenRemoteConfigIsEnabled() = runBlocking<Unit> {
        confirmProductionBackendEnvironment()
        mockBillingAbstract.mockQueryProductDetails()

        // Remote config available (faked 204) + workflows on => components are NOT captured, but the
        // offering still reports a paywall via the cheap presence flag.
        val current = currentOffering()
        assertThat(current.hasPaywall).isTrue()
        assertThat(current.paywallComponents).isNull()
    }

    private suspend fun currentOffering(): Offering =
        Purchases.sharedInstance.awaitOfferings().current ?: fail("Expected a current offering")

    private class RemoteConfigNoOpFake : ForceServerErrorStrategy {
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = false

        override fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
            if (endpoint !is Endpoint.GetRemoteConfig) return null
            // 204 => success no-op: keeps remote config committed-current without fabricating a real RC container.
            return HTTPResult(
                responseCode = RCHTTPStatusCodes.NO_CONTENT,
                payload = "",
                origin = HTTPResult.Origin.BACKEND,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        }
    }
}
