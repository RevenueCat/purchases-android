package com.revenuecat.purchases.integration.identity

import com.revenuecat.purchases.BasePurchasesIntegrationTest
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.configureSdk
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.resetSingleton
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Reproduces a configure-then-logIn race: the anonymous customer info fetch started at configure time waits for
 * the billing client before hitting the backend. If logIn completes in that window, the anonymous response must
 * neither be cached for the logged in user nor be sent to the listener.
 */
abstract class BaseCustomerInfoIdentityRaceIntegrationTest : BasePurchasesIntegrationTest() {

    @Test
    fun anonymousCustomerInfoArrivingAfterLogInDoesNotReplaceLoggedInUser() {
        createIdentifiedUserInBackend()

        val heldQueryPurchasesCallbacks = CopyOnWriteArrayList<(Map<String, StoreTransaction>) -> Unit>()
        restartSdkAnonymouslyHoldingQueryPurchases(heldQueryPurchasesCallbacks)

        val anonymousAppUserID = Purchases.sharedInstance.appUserID
        assertThat(anonymousAppUserID).isNotEqualTo(testUserId)
        assertThat(anonymousAppUserID).startsWith("\$RCAnonymousID:")

        val receivedByListener = CopyOnWriteArrayList<CustomerInfo>()
        var anonymousFetchResult: CustomerInfo? = null
        val anonymousFetchLatch = CountDownLatch(1)
        onActivityReady {
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener {
                receivedByListener.add(it)
            }
            Purchases.sharedInstance.getCustomerInfoWith(
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                onError = { fail("Anonymous customer info fetch should succeed. Error: $it") },
            ) {
                anonymousFetchResult = it
                anonymousFetchLatch.countDown()
            }
        }
        verify(timeout = testTimeout.inWholeMilliseconds) {
            mockBillingAbstract.queryPurchases(anonymousAppUserID, any(), any())
        }
        assertThat(anonymousFetchLatch.count).isEqualTo(1)

        var logInResult: CustomerInfo? = null
        ensureBlockFinishes { latch ->
            onActivityReady {
                Purchases.sharedInstance.logInWith(
                    appUserID = testUserId,
                    onError = { fail("logIn should succeed. Error: $it") },
                ) { customerInfo, _ ->
                    logInResult = customerInfo
                    latch.countDown()
                }
            }
        }
        assertThat(logInResult?.originalAppUserId).isEqualTo(testUserId)

        assertThat(heldQueryPurchasesCallbacks).isNotEmpty
        heldQueryPurchasesCallbacks.forEach { it(emptyMap()) }
        assertThat(anonymousFetchLatch.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)).isTrue
        assertThat(anonymousFetchResult?.originalAppUserId).isEqualTo(anonymousAppUserID)

        var cachedCustomerInfo: CustomerInfo? = null
        ensureBlockFinishes { latch ->
            onActivityReady {
                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.CACHE_ONLY,
                    onError = { fail("Logged in user should have cached customer info. Error: $it") },
                ) {
                    cachedCustomerInfo = it
                    latch.countDown()
                }
            }
        }
        assertThat(cachedCustomerInfo?.originalAppUserId).isEqualTo(testUserId)
        assertThat(receivedByListener).isNotEmpty
        assertThat(receivedByListener.map { it.originalAppUserId }).containsOnly(testUserId)
    }

    private fun createIdentifiedUserInBackend() {
        ensureBlockFinishes { latch ->
            setUpTest { latch.countDown() }
        }
        var customerInfo: CustomerInfo? = null
        ensureBlockFinishes { latch ->
            onActivityReady {
                Purchases.sharedInstance.getCustomerInfoWith(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    onError = { fail("Should be able to fetch customer info. Error: $it") },
                ) {
                    customerInfo = it
                    latch.countDown()
                }
            }
        }
        assertThat(customerInfo?.originalAppUserId).isEqualTo(testUserId)
    }

    private fun restartSdkAnonymouslyHoldingQueryPurchases(
        heldCallbacks: MutableList<(Map<String, StoreTransaction>) -> Unit>,
    ) {
        onActivityReady { context ->
            Purchases.resetSingleton()
            clearAllSharedPreferences(context)
            every {
                mockBillingAbstract.queryPurchases(any(), onSuccess = captureLambda(), onError = any())
            } answers {
                heldCallbacks.add(lambda<(Map<String, StoreTransaction>) -> Unit>().captured)
            }
            Purchases.configureSdk(
                context,
                appUserID = null,
                billingAbstract = mockBillingAbstract,
                forceServerErrorStrategy = forceServerErrorStrategyDelegate,
            )
        }
    }
}
