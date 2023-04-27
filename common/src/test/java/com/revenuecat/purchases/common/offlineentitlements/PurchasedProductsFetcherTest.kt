package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubStoreTransactionFromGooglePurchase
import com.revenuecat.purchases.utils.stubStoreTransactionFromPurchaseHistoryRecord
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasedProductsFetcherTest {
    private lateinit var fetcher: PurchasedProductsFetcher

    private lateinit var deviceCache: DeviceCache
    private lateinit var billing: BillingAbstract
    private lateinit var dateProvider: DateProvider

    private val testDate = Date(1680048626287L) // Tue Mar 28 17:10:26 PDT 2023
    private val testDatePlusOneDay = Date(1680135026287L) // Tue Mar 29 17:10:26 PDT 2023

    private val appUserID = "appUserID"

    val unexpectedOnError: (PurchasesError) -> Unit = {
        fail("Should not have errored")
    }

    @Before
    fun setUp() {
        deviceCache = mockk()
        billing = mockk()
        dateProvider = object : DateProvider {
            override val now: Date
                get() = testDate
        }
        fetcher = PurchasedProductsFetcher(deviceCache, billing, dateProvider)
    }

    @Test
    fun `creates a product with no entitlement from one transaction`() {
        every {
            deviceCache.getProductEntitlementMapping()
        } returns null

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf("product1", "product2"),
            purchaseTime = testDate.time
        )
        val purchaseRecord = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf("product1", "product2"),
            purchaseTime = testDate.time
        )

        mockActivePurchases(activePurchase)
        mockAllPurchases(purchaseRecord)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )
        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(1)
        val purchasedProduct = receivedListOfPurchasedProducts!![0]
        assertThat(purchasedProduct.entitlements).isEmpty()
    }

    @Test
    fun `returns empty list if there are no purchases`() {
        mockEntitlementMapping(mapOf("monthly" to listOf("pro")))

        mockAllPurchases()

        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured.invoke(emptyMap())
        }

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts).isEmpty()
    }

    @Test
    fun `one active purchased product with entitlement`() {
        val productIdentifier = "monthly"
        val productIdentifierToEntitlements = mapOf(productIdentifier to listOf("pro"))
        mockEntitlementMapping(productIdentifierToEntitlements)

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time
        )
        val purchaseRecord = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecord)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(1)

        assertPurchasedProduct(
            receivedListOfPurchasedProducts!![0],
            purchaseRecord,
            productIdentifierToEntitlements,
            isActive = true
        )
    }

    @Test
    fun `one active purchased product with multiple entitlements`() {
        val productIdentifier = "monthly"

        val productIdentifierToEntitlements = mapOf(productIdentifier to listOf("pro", "premium"),)
        mockEntitlementMapping(productIdentifierToEntitlements)

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time
        )
        val purchaseRecord = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecord)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(1)

        assertPurchasedProduct(
            receivedListOfPurchasedProducts!![0],
            purchaseRecord,
            productIdentifierToEntitlements,
            isActive = true
        )
    }

    @Test
    fun `two purchased products, one active with entitlement`() {
        val entitlements = listOf("pro", "premium")
        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"

        val productIdentifierToEntitlements = mapOf(
            productIdentifierMonthly to entitlements,
            productIdentifierAnnual to emptyList()
        )
        mockEntitlementMapping(
            productIdentifierToEntitlements
        )

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordMonthly = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordAnnual = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecordMonthly, purchaseRecordAnnual)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(2)

        val monthlyProduct =
            receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            purchaseRecordMonthly,
            productIdentifierToEntitlements,
            isActive = true
        )

        val annualProduct = receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            purchaseRecordAnnual,
            productIdentifierToEntitlements,
            isActive = false
        )
    }

    @Test
    fun `two purchased products, one active without entitlement`() {
        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"
        val mapOfEntitlements = mapOf<String, List<String>>(
            productIdentifierMonthly to emptyList(),
            productIdentifierAnnual to emptyList()
        )
        mockEntitlementMapping(mapOfEntitlements)

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordMonthly = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordAnnual = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecordMonthly, purchaseRecordAnnual)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(2)

        val monthlyProduct =
            receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            purchaseRecordMonthly,
            mapOfEntitlements,
            isActive = true
        )

        val annualProduct = receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            purchaseRecordAnnual,
            mapOfEntitlements,
            isActive = false
        )
    }

    @Test
    fun `two active purchased products with same entitlement`() {
        val entitlement = "pro"

        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"

        val mapOfEntitlements = mapOf(
            productIdentifierMonthly to listOf(entitlement),
            productIdentifierAnnual to listOf(entitlement)
        )

        mockEntitlementMapping(mapOfEntitlements)

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordMonthly = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordAnnual = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecordMonthly, purchaseRecordAnnual)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(2)

        val monthlyProduct =
            receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            purchaseRecordMonthly,
            mapOfEntitlements,
            isActive = true
        )

        val annualProduct = receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            purchaseRecordAnnual,
            mapOfEntitlements,
            isActive = false
        )
    }

    @Test
    fun `two active purchased products with different entitlement`() {
        val entitlements = listOf("pro", "premium")

        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"

        val mapOfEntitlements = mapOf(
            productIdentifierMonthly to listOf(entitlements[0]),
            productIdentifierAnnual to listOf(entitlements[1])
        )

        mockEntitlementMapping(mapOfEntitlements)

        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordMonthly = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time
        )
        val purchaseRecordAnnual = stubStoreTransactionFromPurchaseHistoryRecord(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time
        )

        mockAllPurchases(purchaseRecordMonthly, purchaseRecordAnnual)
        mockActivePurchases(activePurchase)

        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryPurchasedProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
        assertThat(receivedListOfPurchasedProducts!!.size).isEqualTo(2)

        val monthlyProduct =
            receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            purchaseRecordMonthly,
            mapOfEntitlements,
            isActive = true
        )

        val annualProduct = receivedListOfPurchasedProducts!!.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            purchaseRecordAnnual,
            mapOfEntitlements,
            isActive = false
        )
    }

    // region helpers
    private fun assertPurchasedProduct(
        purchasedProduct: PurchasedProduct,
        purchaseRecord: StoreTransaction,
        productIdentifierToEntitlements: Map<String, List<String>>,
        isActive: Boolean
    ) {
        assertThat(purchasedProduct.productIdentifier).isEqualTo(purchaseRecord.skus[0])
        assertThat(purchasedProduct.storeTransaction).isEqualTo(purchaseRecord)
        assertThat(purchasedProduct.isActive).isEqualTo(isActive)
        assertThat(purchasedProduct.entitlements.size).isEqualTo(productIdentifierToEntitlements[purchasedProduct.productIdentifier]!!.size)
        assertThat(purchasedProduct.entitlements).containsAll(productIdentifierToEntitlements[purchasedProduct.productIdentifier])
        val expiresDate = if (isActive) testDatePlusOneDay else testDate
        assertThat(purchasedProduct.expiresDate)
            .withFailMessage("Expires date should be $expiresDate, but it was ${purchasedProduct.expiresDate}")
            .isEqualTo(expiresDate)
    }

    private fun mockActivePurchases(storeTransaction: StoreTransaction) {
        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            val map = mapOf(storeTransaction.purchaseToken.sha1() to storeTransaction)
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured.invoke(map)
        }
    }

    private fun mockAllPurchases(vararg storeTransactions: StoreTransaction) {
        every {
            billing.queryAllPurchases(
                appUserID,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.invoke(storeTransactions.toList())
        }
    }

    private fun mockEntitlementMapping(
        productIdentifierToEntitlements: Map<String, List<String>>
    ) {
        val mappings = productIdentifierToEntitlements.mapValues { (identifier, entitlements) ->
            ProductEntitlementMapping.Mapping(identifier, null, entitlements)
        }
        val productEntitlementMapping = ProductEntitlementMapping(mappings)
        every {
            deviceCache.getProductEntitlementMapping()
        } returns productEntitlementMapping
    }
    // endregion
}
