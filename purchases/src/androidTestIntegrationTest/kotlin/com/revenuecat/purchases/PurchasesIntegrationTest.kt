package com.revenuecat.purchases

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.factories.ProductDetailsFactory
import com.revenuecat.purchases.factories.PurchaseDetailsFactory
import com.revenuecat.purchases.helpers.mockQuerySkuDetails
import com.revenuecat.purchases.models.skuDetails
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

@RunWith(AndroidJUnit4::class)
class PurchasesIntegrationTest {

    companion object {
        @BeforeClass @JvmStatic
        fun setupClass() {
            if (!canRunIntegrationTests()) {
                error("You need to set required constants in Constants.kt")
            }
        }

        private const val testTimeoutInSeconds = 10L

        private fun canRunIntegrationTests() = Constants.apiKey != "REVENUECAT_API_KEY" &&
            Constants.googlePurchaseToken != "GOOGLE_PURCHASE_TOKEN" &&
            Constants.productIdToPurchase != "PRODUCT_ID_TO_PURCHASE"
    }

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
    fun canFetchOfferings() {
        val lock = CountDownLatch(1)

        val productDetails = ProductDetailsFactory.createProductDetails()
        mockBillingAbstract.mockQuerySkuDetails(querySkuDetailsSubsReturn = listOf(productDetails))

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
        lock.await(testTimeoutInSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero
    }

    @Test
    fun canPurchaseSubsProduct() {
        val lock = CountDownLatch(1)

        val productDetails = ProductDetailsFactory.createProductDetails()
        val purchaseDetails = PurchaseDetailsFactory.createPurchaseDetails()
        mockBillingAbstract.mockQuerySkuDetails(querySkuDetailsSubsReturn = listOf(productDetails))

        onActivityReady { activity ->
            Purchases.sharedInstance.purchaseProductWith(
                activity = activity,
                skuDetails = productDetails.skuDetails,
                onError = { error, _ -> fail("Purchase should be successful. Error: ${error.message}") },
                onSuccess = { _, customerInfo ->
                    assertThat(customerInfo.allPurchaseDatesByProduct.size).isEqualTo(1)
                    assertThat(customerInfo.allPurchaseDatesByProduct.containsKey(productDetails.sku)).isTrue
                    assertThat(customerInfo.entitlements.active.size).isEqualTo(entitlementsToVerify.size)
                    entitlementsToVerify.onEach { entitlementId ->
                        assertThat(customerInfo.entitlements.active[entitlementId]).isNotNull
                    }
                    lock.countDown()
                }
            )
            latestPurchasesUpdatedListener!!.onPurchasesUpdated(listOf(purchaseDetails))
        }
        lock.await(testTimeoutInSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isZero

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                testUserId,
                productDetails,
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
