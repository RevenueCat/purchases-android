package com.revenuecat.purchases

import android.content.Context
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DefaultLocaleProvider
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
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency
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
public class PurchasesIntegrationTest : BasePurchasesIntegrationTest() {

    @Before
    public fun setup() {
        ensureBlockFinishes { latch ->
            setUpTest {
                latch.countDown()
            }
        }
    }

    // region tests

    @Test
    public fun sdkCanBeConfigured() {
        onActivityReady {
            assertThat(Purchases.sharedInstance.appUserID).isNotNull
        }
    }

    @Test
    public fun customerInfoCanBeFetched() {
        confirmProductionBackendEnvironment()

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
    public fun customerInfoCanBeFetchedFromBackendAndThenGottenFromCache() {
        confirmProductionBackendEnvironment()

        val lock = CountDownLatch(1)

        activityScenarioRule.scenario.onActivity {
            Purchases.sharedInstance.getCustomerInfoWith(
                CacheFetchPolicy.FETCH_CURRENT,
                onError = {
                    fail("fetching from backend should be success. Error: ${it.message}")
                },
                onSuccess = { fetchedCustomerInfo ->
                    assertThat(fetchedCustomerInfo.originalSource).isEqualTo(expectedCustomerInfoOriginalSource)
                    assertThat(fetchedCustomerInfo.loadedFromCache).isFalse
                    Purchases.sharedInstance.getCustomerInfoWith(
                        CacheFetchPolicy.CACHE_ONLY,
                        onError = {
                            fail("fetching from cache should be success. Error: ${it.message}")
                        },
                        onSuccess = { cachedCustomerInfo ->
                            assertThat(cachedCustomerInfo).isEqualTo(fetchedCustomerInfo)
                            assertThat(cachedCustomerInfo.originalSource).isEqualTo(expectedCustomerInfoOriginalSource)
                            assertThat(cachedCustomerInfo.loadedFromCache).isTrue
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
    public fun canFetchOfferings() {
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

                    assertThat(offerings.current?.paywall).isNull()
                    assertThat(offerings.current?.paywallComponents).isNotNull

                    lock.countDown()
                },
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    public fun offeringsArePersistedAndUsedOnServerErrors() {
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

        simulateSdkRestart(activity, forceServerErrorsStrategy = ForceServerErrorStrategy.failAll)

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
    public fun canPurchaseSubsProduct() {
        performPurchase()
    }

    @Test
    public fun canPurchaseSubsProductAndThenFetchCustomerInfo() {
        performPurchase()

        ensureBlockFinishes { latch ->
            Purchases.sharedInstance.getCustomerInfoWith(
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                onError = { fail("Expected success. Got error: $it") },
                onSuccess = { customerInfo ->
                    verifyCustomerInfoHasPurchase(customerInfo)
                    assertThat(customerInfo.originalSource).isEqualTo(expectedCustomerInfoOriginalSource)
                    assertThat(customerInfo.loadedFromCache).isFalse
                    latch.countDown()
                },
            )
        }
    }

    @Test
    public fun testGetVirtualCurrenciesWithBalancesOfZero() {
        // Virtual Currencies aren't supported by the load shedder yet, so we don't want to run
        // VC tests in the load shedder integration tests
        confirmProductionBackendEnvironment()

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
                        validateAllZeroBalances(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    public fun testGetVirtualCurrenciesWithBalancesWithSomeNonZeroValues() {
        // Virtual Currencies aren't supported by the load shedder yet, so we don't want to run
        // VC tests in the load shedder integration tests
        confirmProductionBackendEnvironment()

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
                        validateNonZeroBalances(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    public fun testGettingVirtualCurrenciesForNewUserReturnsVCsWith0Balance() {
        // Virtual Currencies aren't supported by the load shedder yet, so we don't want to run
        // VC tests in the load shedder integration tests
        confirmProductionBackendEnvironment()

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
                        validateAllZeroBalances(virtualCurrencies = virtualCurrencies)
                        lock.countDown()
                    },
                )
            },
        )

        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    public fun testCachedVirtualCurrencies() {
        // Virtual Currencies aren't supported by the load shedder yet, so we don't want to run
        // VC tests in the load shedder integration tests
        confirmProductionBackendEnvironment()

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
                        validateNonZeroBalances(virtualCurrencies = virtualCurrencies)

                        var cachedVirtualCurrencies = Purchases.sharedInstance.cachedVirtualCurrencies
                        validateNonZeroBalances(virtualCurrencies = cachedVirtualCurrencies)

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

    private fun performPurchase() {
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
                    verifyCustomerInfoHasPurchase(customerInfo)
                    assertThat(customerInfo.originalSource).isEqualTo(expectedCustomerInfoOriginalSource)
                    assertThat(customerInfo.loadedFromCache).isFalse
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

    private fun verifyCustomerInfoHasPurchase(customerInfo: CustomerInfo) {
        assertThat(customerInfo.allPurchaseDatesByProduct.size).isEqualTo(1)
        val productId = customerInfo.allPurchaseDatesByProduct.keys.first()
        val expectedProductId = "${Constants.productIdToPurchase}:${Constants.basePlanIdToPurchase}"
        assertThat(productId).isEqualTo(expectedProductId)
        assertThat(customerInfo.entitlements.active.size).isEqualTo(entitlementsToVerify.size)
        entitlementsToVerify.onEach { entitlementId ->
            assertThat(customerInfo.entitlements.active[entitlementId]).isNotNull
        }
    }

    private fun validateAllZeroBalances(virtualCurrencies: VirtualCurrencies?) {
        validateVirtualCurrenciesObject(
            virtualCurrencies = virtualCurrencies,
            testVCBalance = 0,
            testVC2Balance = 0,
        )
    }

    private fun validateNonZeroBalances(virtualCurrencies: VirtualCurrencies?) {
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
        assert(virtualCurrencies!!.all.count() == 3)

        val expectedTestVirtualCurrency = VirtualCurrency(
            code = "TEST",
            name = "Test Currency",
            balance = testVCBalance,
            serverDescription = "This is a test currency",
        )
        assertThat(virtualCurrencies["TEST"]).isEqualTo(expectedTestVirtualCurrency)

        val expectedTestVirtualCurrency2 = VirtualCurrency(
            code = "TEST2",
            name = "Test Currency 2",
            balance = testVC2Balance,
            serverDescription = "This is test currency 2",
        )
        assertThat(virtualCurrencies["TEST2"]).isEqualTo(expectedTestVirtualCurrency2)

        val expectedTestVirtualCurrency3 = VirtualCurrency(
            code = "TEST3",
            name = "Test Currency 3",
            balance = testVC3Balance,
            serverDescription = null,
        )
        assertThat(virtualCurrencies["TEST3"]).isEqualTo(expectedTestVirtualCurrency3)
    }

    // endregion

    // region reachability
    // These tests are to verify that the other tests can reach the RC servers.

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    public fun failsWithUnknownHostIfInvalidSubdomain() {
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
    public fun failsWithUnauthorizedIfValidURLButInvalidAuth() {
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
            every { store } returns Store.PLAY_STORE
            every { platformInfo } returns PlatformInfo("native", "3.2.0")
            every { languageTag } returns "en"
            every { versionName } returns "3.2.0"
            every { packageName } returns "com.revenuecat.purchase_integration_tests"
            every { finishTransactions } returns true
            every { customEntitlementComputation } returns false
            every { isDebugBuild } returns true
            every { isAppBackgrounded } returns false
            every { runningTests } returns true
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
            localeProvider = DefaultLocaleProvider(),
        )
    }

    // endregion reachability
}
