package com.revenuecat.purchases.cachedofferings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class CachedOfferingsUsageIntegrationTest : BasePurchasesIntegrationTest() {

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
    fun cachedOfferingsAreUsedWhenCachedOfferingsAndServerErrorWith5xx() = runTest {
        val networkOfferings = Purchases.sharedInstance.awaitOfferings()

        simulateSdkRestart(activity, forceServerErrorsStrategy = ForceServerErrorStrategy.failAll)

        val cachedOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(cachedOfferings).isEqualTo(networkOfferings)
    }

    @Test
    fun cachedOfferingsAreUsedWhenCachedOfferingsAndServerCannotBeReached() = runTest {
        val networkOfferings = Purchases.sharedInstance.awaitOfferings()

        simulateSdkRestart(
            activity,
            forceServerErrorsStrategy = object : ForceServerErrorStrategy {
                override val serverErrorURL: String
                    get() = "http://localhost:100/unreachable-address"

                override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                    return true
                }
            },
        )

        val cachedOfferings = Purchases.sharedInstance.awaitOfferings()

        assertThat(cachedOfferings).isEqualTo(networkOfferings)
    }

    @Test
    fun cachedOfferingsAreNotUsedWhenCachedOfferingsAndErrorWith4xx() = runTest {
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
