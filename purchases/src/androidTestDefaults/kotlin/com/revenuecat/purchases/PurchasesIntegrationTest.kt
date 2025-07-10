package com.revenuecat.purchases

import android.content.Context
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SigningManager
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQueryProductDetails
import com.revenuecat.purchases.interfaces.StorefrontProvider
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
@RunWith(AndroidJUnit4::class)
class PurchasesIntegrationTest : BasePurchasesIntegrationTest() {

    @Before
    fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    // region tests

    @Test
    fun sdkCanBeConfigured() {
        onActivityReady {
            assertThat(Purchases.sharedInstance.appUserID).isNotNull
        }
    }

    @Test
    fun customerInfoCanBeFetched() {
        val lock = CountDownLatch(1)

        onActivityReady {
            Purchases.sharedInstance.getCustomerInfoWith({
                fail("should be success. Error: ${it.message}")
            }) {
                lock.countDown()
            }
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun customerInfoCanBeFetchedFromBackendAndThenGottenFromCache() {
        val lock = CountDownLatch(1)

        activityScenarioRule.scenario.onActivity {
            Purchases.sharedInstance.getCustomerInfoWith(
                CacheFetchPolicy.FETCH_CURRENT,
                onError = {
                    fail("fetching from backend should be success. Error: ${it.message}")
                },
                onSuccess = { fetchedCustomerInfo ->
                    Purchases.sharedInstance.getCustomerInfoWith(
                        CacheFetchPolicy.CACHE_ONLY,
                        onError = {
                            fail("fetching from cache should be success. Error: ${it.message}")
                        },
                        onSuccess = { cachedCustomerInfo ->
                            assertThat(cachedCustomerInfo).isEqualTo(fetchedCustomerInfo)
                            lock.countDown()
                        },
                    )
                },
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun canFetchOfferings() {
        val lock = CountDownLatch(1)

        mockBillingAbstract.mockQueryProductDetails()

        onActivityReady {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error -> fail("Get offerings should be successful. Error: ${error.message}") },
                onSuccess = { offerings ->
                    assertThat(offerings.current).isNotNull
                    assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
                    assertThat(offerings.current?.availablePackages?.get(0)?.product?.sku)
                        .isEqualTo(Constants.productIdToPurchase)

                    assertThat(offerings.current?.metadata).isNotNull
                    assertThat(offerings.current?.metadata?.get("dontdeletethis")).isEqualTo("useforintegrationtesting")

                    lock.countDown()
                },
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun offeringsArePersistedAndUsedOnServerErrors() {
        mockBillingAbstract.mockQueryProductDetails()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error -> fail("Get offerings should succeed. Error: ${error.underlyingErrorMessage}") },
                onSuccess = { offerings ->
                    assertThat(offerings.current).isNotNull
                    assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
                    assertThat(offerings.current?.availablePackages?.get(0)?.product?.sku)
                        .isEqualTo(Constants.productIdToPurchase)
                    latch.countDown()
                },
            )
        }

        simulateSdkRestart(activity, forceServerErrors = true)

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error -> fail("Get offerings should succeed. Error: ${error.underlyingErrorMessage}") },
                onSuccess = { newOfferings ->
                    assertThat(newOfferings.current).isNotNull
                    assertThat(newOfferings.current?.availablePackages?.size).isEqualTo(1)
                    assertThat(newOfferings.current?.availablePackages?.get(0)?.product?.sku)
                        .isEqualTo(Constants.productIdToPurchase)

                    latch.countDown()
                },
            )
        }
    }

    @Test
    fun canPurchaseSubsProduct() {
        val lock = CountDownLatch(1)

        val storeProduct = StoreProductFactory.createGoogleStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQueryProductDetails(queryProductDetailsSubsReturn = listOf(storeProduct))

        onActivityReady { activity ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = PurchaseParams.Builder(activity, storeProduct).build(),
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { transaction, customerInfo ->
                    assertThat(transaction).isEqualTo(storeTransaction)
                    assertThat(customerInfo.allPurchaseDatesByProduct.size).isEqualTo(1)
                    val productId = customerInfo.allPurchaseDatesByProduct.keys.first()
                    val expectedProductId = "${Constants.productIdToPurchase}:${Constants.basePlanIdToPurchase}"
                    assertThat(productId).isEqualTo(expectedProductId)
                    assertThat(customerInfo.entitlements.active.size).isEqualTo(entitlementsToVerify.size)
                    entitlementsToVerify.onEach { entitlementId ->
                        assertThat(customerInfo.entitlements.active[entitlementId]).isNotNull
                    }
                    lock.countDown()
                },
            )
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                testUserId,
                match {
                    it is GooglePurchasingData.Subscription &&
                        storeProduct is GoogleStoreProduct &&
                        it.productId == storeProduct.productId &&
                        it.optionId == storeProduct.basePlanId
                },
                replaceProductInfo = null,
                presentedOfferingContext = null,
                isPersonalizedPrice = null,
            )
        }
    }

    @Test
    fun testGetVirtualCurrenciesWithBalancesOfZero() {
        val appUserIDWith0BalanceCurrencies = "integrationTestUserWithAllBalancesEqualTo0"
        val lock = CountDownLatch(1)

        Purchases.sharedInstance.logInWith(
            appUserID = appUserIDWith0BalanceCurrencies,
            onError = { error -> fail("should have been able to login. Error: $error") },
            onSuccess = { _, created ->
                assertThat(created).isFalse() // This user should already exist

                Purchases.sharedInstance.invalidateVirtualCurrenciesCache()

                Purchases.sharedInstance.getVirtualCurrenciesWith(
                    onError = { error -> fail("should be success. Error: $error") },
                    onSuccess = { virtualCurrencies ->
                        validateAllZeroBalanceVirtualCurrenciesObject(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun testGetVirtualCurrenciesWithBalancesWithSomeNonZeroValues() {
        val appUserIDWith0BalanceCurrencies = "integrationTestUserWithAllBalancesNonZero"
        val lock = CountDownLatch(1)

        Purchases.sharedInstance.logInWith(
            appUserID = appUserIDWith0BalanceCurrencies,
            onError = { error -> fail("should have been able to login. Error: $error") },
            onSuccess = { _, created ->
                assertThat(created).isFalse() // This user should already exist

                Purchases.sharedInstance.invalidateVirtualCurrenciesCache()

                Purchases.sharedInstance.getVirtualCurrenciesWith(
                    onError = { error -> fail("should be success. Error: $error") },
                    onSuccess = { virtualCurrencies ->
                        validateAllNonZeroBalanceVirtualCurrenciesObject(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun testGettingVirtualCurrenciesForNewUserReturnsVCsWith0Balance() {
        val newAppUserID = "integrationTestUser_${UUID.randomUUID()}"
        val lock = CountDownLatch(1)

        Purchases.sharedInstance.logInWith(
            appUserID = newAppUserID,
            onError = { error -> fail("should have been able to login. Error: $error") },
            onSuccess = { _, created ->
                assertThat(created).isTrue() // This user should be new

                Purchases.sharedInstance.invalidateVirtualCurrenciesCache()

                Purchases.sharedInstance.getVirtualCurrenciesWith(
                    onError = { error -> fail("should be success. Error: $error") },
                    onSuccess = { virtualCurrencies ->
                        validateAllZeroBalanceVirtualCurrenciesObject(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun testCachedVirtualCurrencies() {
        val appUserID = "integrationTestUserWithAllBalancesNonZero"
        val lock = CountDownLatch(1)

        Purchases.sharedInstance.logInWith(
            appUserID = appUserID,
            onError = { error -> fail("should have been able to login. Error: $error") },
            onSuccess = { _, created ->
                assertThat(created).isFalse() // This user should be already exist

                Purchases.sharedInstance.invalidateVirtualCurrenciesCache()

                Purchases.sharedInstance.getVirtualCurrenciesWith(
                    onError = { error -> fail("should be success. Error: $error") },
                    onSuccess = { virtualCurrencies ->
                        validateAllNonZeroBalanceVirtualCurrenciesObject(virtualCurrencies = virtualCurrencies)

                        var cachedVirtualCurrencies = Purchases.sharedInstance.cachedVirtualCurrencies
                        validateAllNonZeroBalanceVirtualCurrenciesObject(virtualCurrencies = cachedVirtualCurrencies)

                        Purchases.sharedInstance.invalidateVirtualCurrenciesCache()
                        cachedVirtualCurrencies = Purchases.sharedInstance.cachedVirtualCurrencies
                        assertThat(cachedVirtualCurrencies).isNull()

                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    private fun validateAllZeroBalanceVirtualCurrenciesObject(virtualCurrencies: VirtualCurrencies?) {
        validateVirtualCurrenciesObject(
            virtualCurrencies = virtualCurrencies,
            testVCBalance = 0,
            testVC2Balance = 0,
        )
    }

    private fun validateAllNonZeroBalanceVirtualCurrenciesObject(virtualCurrencies: VirtualCurrencies?) {
        validateVirtualCurrenciesObject(
            virtualCurrencies = virtualCurrencies,
            testVCBalance = 100,
            testVC2Balance = 777,
        )
    }

    @Suppress("MagicNumber")
    private fun validateVirtualCurrenciesObject(
        virtualCurrencies: VirtualCurrencies?,
        testVCBalance: Int,
        testVC2Balance: Int,
        testVC3Balance: Int = 0,
    ) {
        assertThat(virtualCurrencies).isNotNull()
        assertThat(virtualCurrencies!!.all.size).isEqualTo(3)

        val testVCCode = "TEST"
        assertThat(virtualCurrencies.all[testVCCode]).isNotNull()
        assertThat(virtualCurrencies.all[testVCCode]?.balance).isEqualTo(testVCBalance)
        assertThat(virtualCurrencies.all[testVCCode]?.code).isEqualTo(testVCCode)
        assertThat(virtualCurrencies.all[testVCCode]?.name).isEqualTo("Test Currency")
        assertThat(virtualCurrencies.all[testVCCode]?.serverDescription).isEqualTo("This is a test currency")

        val testVCCode2 = "TEST2"
        assertThat(virtualCurrencies.all[testVCCode2]).isNotNull()
        assertThat(virtualCurrencies.all[testVCCode2]?.balance).isEqualTo(testVC2Balance)
        assertThat(virtualCurrencies.all[testVCCode2]?.code).isEqualTo(testVCCode2)
        assertThat(virtualCurrencies.all[testVCCode2]?.name).isEqualTo("Test Currency 2")
        assertThat(virtualCurrencies.all[testVCCode2]?.serverDescription).isEqualTo("This is test currency 2")

        val testVCCode3 = "TEST3"
        assertThat(virtualCurrencies.all[testVCCode3]).isNotNull()
        assertThat(virtualCurrencies.all[testVCCode3]?.balance).isEqualTo(testVC3Balance)
        assertThat(virtualCurrencies.all[testVCCode3]?.code).isEqualTo(testVCCode3)
        assertThat(virtualCurrencies.all[testVCCode3]?.name).isEqualTo("Test Currency 3")
        assertThat(virtualCurrencies.all[testVCCode3]?.serverDescription).isNull()
    }

    // endregion

    // region reachability
    // These tests are to verify that the other tests can reach the RC servers.

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun failsWithUnknownHostIfInvalidSubdomain() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        ensureBlockFinishes { latch ->
            onActivityReady {
                val httpClient = createHttpClient(activity)
                GlobalScope.launch(Dispatchers.IO) {
                    assertThatExceptionOfType(UnknownHostException::class.java).isThrownBy {
                        httpClient.performRequest(
                            baseURL = URL("https://invalid-base-url.revenuecat.com/"),
                            endpoint = Endpoint.GetOfferings("test-user-id"),
                            body = null,
                            postFieldsToSign = null,
                            requestHeaders = emptyMap(),
                        )
                    }
                    latch.countDown()
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun failsWithUnauthorizedIfValidURLButInvalidAuth() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        ensureBlockFinishes { latch ->
            onActivityReady {
                val httpClient = createHttpClient(activity)
                GlobalScope.launch(Dispatchers.IO) {
                    val result = httpClient.performRequest(
                        baseURL = URL("https://api.revenuecat.com/"),
                        endpoint = Endpoint.GetOfferings("test-user-id"),
                        body = null,
                        postFieldsToSign = null,
                        requestHeaders = emptyMap(),
                    )
                    assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
                    latch.countDown()
                }
            }
        }
    }

    private fun createHttpClient(context: Context): HTTPClient {
        val appConfig = mockk<AppConfig>().apply {
            every { forceServerErrors } returns false
            every { store } returns Store.PLAY_STORE
            every { platformInfo } returns PlatformInfo("native", "3.2.0")
            every { languageTag } returns "en"
            every { versionName } returns "3.2.0"
            every { packageName } returns "com.revenuecat.purchase_integration_tests"
            every { finishTransactions } returns true
            every { customEntitlementComputation } returns false
            every { isDebugBuild } returns true
            every { isAppBackgrounded } returns false
        }
        return HTTPClient(
            appConfig = appConfig,
            eTagManager = ETagManager(context),
            diagnosticsTrackerIfEnabled = null,
            signingManager = mockk<SigningManager>().apply {
                every { shouldVerifyEndpoint(any()) } returns false
            },
            storefrontProvider = object : StorefrontProvider {
                override fun getStorefront(): String {
                    return "test-storefront"
                }
            },
        )
    }

    // endregion reachability
}
