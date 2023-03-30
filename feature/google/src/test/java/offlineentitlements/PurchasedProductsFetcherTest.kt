package com.revenuecat.purchases.google.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.offlineentitlements.PurchasedProduct
import com.revenuecat.purchases.common.offlineentitlements.PurchasedProductsFetcher
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
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

        val activePurchase =
            stubGooglePurchase(productIds = listOf("product1", "product2")).toStoreTransaction(ProductType.SUBS, null)
        val purchaseRecord =
            stubPurchaseHistoryRecord(productIds = listOf("product1", "product2")).toStoreTransaction(ProductType.SUBS)

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
        mockEntitlementMapping(
            ProductEntitlementMapping.Mapping("monthly", listOf("pro"))
        )

        mockAllPurchases()
        mockActivePurchases()

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
        val entitlements = listOf("pro")
        val productIdentifier = "monthly"
        mockEntitlementMapping(
            ProductEntitlementMapping.Mapping(productIdentifier, entitlements)
        )

        val activePurchase =
            stubGooglePurchase(productIds = listOf(productIdentifier)).toStoreTransaction(ProductType.SUBS, null)
        val purchaseRecord =
            stubPurchaseHistoryRecord(productIds = listOf(productIdentifier)).toStoreTransaction(ProductType.SUBS)

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
        val purchasedProduct = receivedListOfPurchasedProducts!![0]
        assertThat(purchasedProduct.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchasedProduct.storeTransaction).isEqualTo(purchaseRecord)
        assertThat(purchasedProduct.isActive).isEqualTo(true)
        assertThat(purchasedProduct.entitlements.size).isEqualTo(1)
        assertThat(purchasedProduct.entitlements).containsAll(entitlements)
        assertThat(purchasedProduct.expiresDate)
            .withFailMessage("Expires date should be $testDate, but it was ${purchasedProduct.expiresDate}")
            .isEqualTo(testDatePlusOneDay)
    }

    @Test
    fun `one active purchased product with multiple entitlements`() {
        val entitlements = listOf("pro", "premium")
        val productIdentifier = "monthly"
        mockEntitlementMapping(
            ProductEntitlementMapping.Mapping(productIdentifier, entitlements)
        )

        val activePurchase =
            stubGooglePurchase(productIds = listOf(productIdentifier)).toStoreTransaction(ProductType.SUBS, null)
        val purchaseRecord =
            stubPurchaseHistoryRecord(productIds = listOf(productIdentifier)).toStoreTransaction(ProductType.SUBS)

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
        val purchasedProduct = receivedListOfPurchasedProducts!![0]
        assertThat(purchasedProduct.productIdentifier).isEqualTo(productIdentifier)
        assertThat(purchasedProduct.storeTransaction).isEqualTo(purchaseRecord)
        assertThat(purchasedProduct.isActive).isEqualTo(true)
        assertThat(purchasedProduct.entitlements.size).isEqualTo(2)
        assertThat(purchasedProduct.entitlements).containsAll(entitlements)
        assertThat(purchasedProduct.expiresDate)
            .withFailMessage("Expires date should be $testDate, but it was ${purchasedProduct.expiresDate}")
            .isEqualTo(testDatePlusOneDay)
    }

    @Test
    fun `two purchased products, one active with entitlement`() {
    }

    @Test
    fun `two active purchased products with same entitlement`() {
    }

    @Test
    fun `two active purchased products with different entitlement`() {
    }

    private fun mockActivePurchases(vararg storeTransactions: StoreTransaction) {
        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            val map = storeTransactions.associateBy { it.purchaseToken.sha1() }
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
        vararg mapping: ProductEntitlementMapping.Mapping
    ) {
        every {
            deviceCache.getProductEntitlementMapping()
        } returns ProductEntitlementMapping(mapping.toList())
    }
}