package com.revenuecat.purchases

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.factories.StoreTransactionFactory
import com.revenuecat.purchases.helpers.mockQuerySkuDetails
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PurchasesIntegrationTest {

    companion object {
        @BeforeClass @JvmStatic
        fun setupClass() {
            if (!canRunIntegrationTests()) {
                error("You need to set required constants in Constants.kt")
            }
        }

        private fun canRunIntegrationTests() = Constants.apiKey != "REVENUECAT_API_KEY" &&
            Constants.googlePurchaseToken != "GOOGLE_PURCHASE_TOKEN" &&
            Constants.productIdToPurchase != "PRODUCT_ID_TO_PURCHASE"
    }

    private val testTimeout = 10.seconds
    private val currentTimestamp = Date().time
    private val testUserId = "android-integration-test-$currentTimestamp"
    private val entitlementsToVerify = Constants.activeEntitlementIdsToVerify
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    private val proxyUrl = Constants.proxyUrl.takeIf { it != "NO_PROXY_URL" }

    private lateinit var mockBillingAbstract: BillingAbstract

    private var latestPurchasesUpdatedListener: BillingAbstract.PurchasesUpdatedListener? = null
    private var latestStateListener: BillingAbstract.StateListener? = null

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    @Before
    fun setup() {
        latestPurchasesUpdatedListener = null
        latestStateListener = null

        onActivityReady {
            clearAllSharedPreferences(it)

            mockBillingAbstract = mockk<BillingAbstract>(relaxed = true).apply {
                every { purchasesUpdatedListener = any() } answers { latestPurchasesUpdatedListener = firstArg() }
                every { stateListener = any() } answers { latestStateListener = firstArg() }
            }

            proxyUrl?.let { urlString ->
                Purchases.proxyURL = URL(urlString)
            }

            Purchases.configure(
                PurchasesConfiguration.Builder(it, Constants.apiKey)
                    .appUserID(testUserId)
                    .build(),
                mockBillingAbstract
            )
        }
    }

    @After
    fun tearDown() {
        Purchases.sharedInstance.close()
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
                        }
                    )
                }
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun canFetchOfferings() {
        val lock = CountDownLatch(1)

        val storeProduct = StoreProductFactory.createStoreProduct()
        mockBillingAbstract.mockQuerySkuDetails(querySkuDetailsSubsReturn = listOf(storeProduct))

        onActivityReady {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error -> fail("Get offerings should be successful. Error: ${error.message}") },
                onSuccess = { offerings ->
                    assertThat(offerings.current).isNotNull
                    assertThat(offerings.current?.availablePackages?.size).isEqualTo(1)
                    assertThat(offerings.current?.monthly?.product?.sku).isEqualTo(Constants.productIdToPurchase)

                    lock.countDown()
                }
            )
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun canPurchaseSubsProduct() {
        val lock = CountDownLatch(1)

        val storeProduct = StoreProductFactory.createStoreProduct()
        val storeTransaction = StoreTransactionFactory.createStoreTransaction()
        mockBillingAbstract.mockQuerySkuDetails(querySkuDetailsSubsReturn = listOf(storeProduct))

        onActivityReady { activity ->
            Purchases.sharedInstance.purchaseProductWith(
                activity = activity,
                storeProduct = storeProduct,
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { transaction, customerInfo ->
                    assertThat(transaction).isEqualTo(storeTransaction)
                    assertThat(customerInfo.allPurchaseDatesByProduct.size).isEqualTo(1)
                    assertThat(customerInfo.allPurchaseDatesByProduct.containsKey(storeProduct.sku)).isTrue
                    assertThat(customerInfo.entitlements.active.size).isEqualTo(entitlementsToVerify.size)
                    entitlementsToVerify.onEach { entitlementId ->
                        assertThat(customerInfo.entitlements.active[entitlementId]).isNotNull
                    }
                    lock.countDown()
                }
            )
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(storeTransaction))
        }
        lock.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                testUserId,
                storeProduct,
                replaceSkuInfo = null,
                presentedOfferingIdentifier = null
            )
        }
    }

    // endregion

    // region helpers

    private fun onActivityReady(block: (MainActivity) -> Unit) {
        activityScenarioRule.scenario.onActivity(block)
    }

    private fun clearAllSharedPreferences(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        context.getSharedPreferences(
            "${context.packageName}_preferences_etags",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        context.getSharedPreferences(
            "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
    }

    // endregion
}
