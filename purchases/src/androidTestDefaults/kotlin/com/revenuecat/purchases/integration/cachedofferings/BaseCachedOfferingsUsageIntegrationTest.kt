package com.revenuecat.purchases.integration.cachedofferings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URL

abstract class BaseCachedOfferingsUsageIntegrationTest : BasePurchasesIntegrationTest() {

    private companion object {
        const val UNREACHABLE_URL = "http://localhost:100/unreachable-address"

        /**
         * These tests restart the SDK twice against the real backend, and `getOfferings` only returns once the
         * `/v1/config` paywall data is ready, which can include downloading config blobs from the config CDN.
         * `runTest`'s 10s default is a deadlock guard, not a latency budget, and it is too tight for that.
         */
        const val BACKEND_TEST_TIMEOUT_MS = 60_000L
    }

    /** The RevenueCat API is unreachable. The config CDN, a different host, still is. */
    private val apiUnreachable = object : ForceServerErrorStrategy {
        override val serverErrorURL: String
            get() = UNREACHABLE_URL

        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = true
    }

    /** Nothing is reachable: neither the API nor any direct download (config blobs). */
    private val fullyOffline = object : ForceServerErrorStrategy {
        override val serverErrorURL: String
            get() = UNREACHABLE_URL

        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = true

        override fun shouldForceConnectionFailure(url: String): Boolean = true
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }

        mockBillingAbstract.mockQueryProductDetails()
    }

    @Test
    fun cachedOfferingsAreUsedWhenCachedOfferingsAndServerErrorWith5xx() = runTest(
        dispatchTimeoutMs = BACKEND_TEST_TIMEOUT_MS,
    ) {
        val networkOfferings = Purchases.sharedInstance.awaitOfferings()

        simulateSdkRestart(activity, forceServerErrorsStrategy = ForceServerErrorStrategy.failAll)

        val cachedOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(cachedOfferings).isEqualTo(networkOfferings)
    }

    @Test
    fun cachedOfferingsAreUsedWhenCachedOfferingsAndServerCannotBeReached() = runTest(
        dispatchTimeoutMs = BACKEND_TEST_TIMEOUT_MS,
    ) {
        val networkOfferings = Purchases.sharedInstance.awaitOfferings()

        simulateSdkRestart(activity, forceServerErrorsStrategy = apiUnreachable)

        val cachedOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(cachedOfferings).isEqualTo(networkOfferings)
    }

    /**
     * Unlike the test above, this one keeps `runTest`'s tight default budget on purpose: with no host reachable
     * there is nothing legitimate left to wait for, so it also guards against the paywall-config readiness gate
     * stranding or slowing down a `getOfferings` that the disk cache can already answer.
     */
    @Test
    fun cachedOfferingsAreUsedWhenFullyOffline() = runTest {
        val networkOfferings = Purchases.sharedInstance.awaitOfferings()

        // Config blobs cached by the first fetch would let the readiness gate resolve without the network,
        // hiding the very wait this test exists to catch.
        RemoteConfigBlobStore(activity).clear()
        simulateSdkRestart(activity, forceServerErrorsStrategy = fullyOffline)

        val cachedOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(cachedOfferings).isEqualTo(networkOfferings)
    }

    @Test
    fun cachedOfferingsAreNotUsedWhenCachedOfferingsAndErrorWith4xx() = runTest(
        dispatchTimeoutMs = BACKEND_TEST_TIMEOUT_MS,
    ) {
        Purchases.sharedInstance.awaitOfferings()

        simulateSdkRestart(
            activity,
            forceServerErrorsStrategy = object : ForceServerErrorStrategy {
                override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                    return false
                }

                override fun fakeResponseWithoutPerformingRequest(baseURL: URL, endpoint: Endpoint): HTTPResult? {
                    if (endpoint is Endpoint.GetOfferings) {
                        return HTTPResult(
                            responseCode = 401,
                            payload = "{}",
                            origin = HTTPResult.Origin.BACKEND,
                            requestDate = null,
                            verificationResult = VerificationResult.VERIFIED_ON_DEVICE,
                            isLoadShedderResponse = false,
                            isFallbackURL = false,
                        )
                    }
                    return null
                }
            },
        )

        try {
            Purchases.sharedInstance.awaitOfferings()
            fail("Expected to error")
        } catch (e: PurchasesException) {
            assertThat(e.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
            assertThat(e.underlyingErrorMessage).isEqualTo("Backend Code: N/A - ")
        }
    }
}
