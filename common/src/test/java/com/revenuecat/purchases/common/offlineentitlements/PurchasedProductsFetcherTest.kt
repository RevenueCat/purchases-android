package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import com.revenuecat.purchases.utils.stubStoreTransactionFromGooglePurchase
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

    private val unexpectedOnError: (PurchasesError) -> Unit = {
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
    fun `checks onError callback is called`() {
        every {
            deviceCache.getProductEntitlementMapping()
        } returns null
        var error: PurchasesError? = null

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = { fail("Should not have succeeded") },
            onError = { error = it },
        )

        assertThat(error).isNotNull
    }

    @Test
    fun `fails fetching products if product entitlement mappings not available`() {
        every {
            deviceCache.getProductEntitlementMapping()
        } returns null
        var error: PurchasesError? = null

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = { fail("Should not have succeeded") },
            onError = { error = it },
        )

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.CustomerInfoError)
        assertThat(error?.underlyingErrorMessage).isEqualTo(
            OfflineEntitlementsStrings.PRODUCT_ENTITLEMENT_MAPPING_REQUIRED
        )
    }

    @Test
    fun `checks onSuccess callback is called`() {
        mockEntitlementMapping(mapOf("monthly" to listOf("pro")))
        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured.invoke(emptyMap())
        }
        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts).isNotNull
    }

    @Test
    fun `returns empty list if there are no purchases`() {
        mockEntitlementMapping(mapOf("monthly" to listOf("pro")))
        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured.invoke(emptyMap())
        }
        var receivedListOfPurchasedProducts: List<PurchasedProduct>? = null

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts).isEmpty()
    }

    @Test
    fun `one active purchased product with single entitlement`() {
        val productIdentifier = "monthly"
        val productIdentifierToEntitlements = mapOf(productIdentifier to listOf("pro"))
        mockEntitlementMapping(productIdentifierToEntitlements)
        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time,
        )
        mockActivePurchases(listOf(activePurchase))
        var receivedListOfPurchasedProducts: List<PurchasedProduct> = emptyList()

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts.size).isEqualTo(1)
        assertPurchasedProduct(
            receivedListOfPurchasedProducts[0],
            activePurchase,
            productIdentifierToEntitlements,
        )
    }

    @Test
    fun `one active purchased product with multiple entitlements`() {
        val productIdentifier = "monthly"
        val productIdentifierToEntitlements = mapOf(productIdentifier to listOf("pro", "premium"))
        mockEntitlementMapping(productIdentifierToEntitlements)
        val activePurchase = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifier),
            purchaseTime = testDate.time,
        )
        mockActivePurchases(listOf(activePurchase))
        var receivedListOfPurchasedProducts: List<PurchasedProduct> = emptyList()

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts.size).isEqualTo(1)
        assertPurchasedProduct(
            receivedListOfPurchasedProducts[0],
            activePurchase,
            productIdentifierToEntitlements,
        )
    }

    @Test
    fun `two active purchased products with same entitlement`() {
        val entitlement = "pro"
        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"
        val mapOfEntitlements = mapOf(
            productIdentifierMonthly to listOf(entitlement),
            productIdentifierAnnual to listOf(entitlement),
        )
        mockEntitlementMapping(mapOfEntitlements)
        val activePurchaseMonthly = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time,
            purchaseToken = "test-token-1",
        )
        val activePurchaseAnnual = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time,
            purchaseToken = "test-token-2",
        )
        mockActivePurchases(listOf(activePurchaseMonthly, activePurchaseAnnual))
        var receivedListOfPurchasedProducts: List<PurchasedProduct> = emptyList()

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts.size).isEqualTo(2)
        val monthlyProduct =
            receivedListOfPurchasedProducts.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            activePurchaseMonthly,
            mapOfEntitlements,
        )
        val annualProduct = receivedListOfPurchasedProducts.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            activePurchaseAnnual,
            mapOfEntitlements,
        )
    }

    @Test
    fun `two active purchased products with different entitlement`() {
        val entitlements = listOf("pro", "premium")
        val productIdentifierMonthly = "monthly"
        val productIdentifierAnnual = "annual"
        val mapOfEntitlements = mapOf(
            productIdentifierMonthly to listOf(entitlements[0]),
            productIdentifierAnnual to listOf(entitlements[1]),
        )
        mockEntitlementMapping(mapOfEntitlements)
        val activePurchaseMonthly = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierMonthly),
            purchaseTime = testDate.time,
            purchaseToken = "test-token-1",
        )
        val activePurchaseAnnual = stubStoreTransactionFromGooglePurchase(
            productIds = listOf(productIdentifierAnnual),
            purchaseTime = testDate.time,
            purchaseToken = "test-token-2",
        )
        mockActivePurchases(listOf(activePurchaseMonthly, activePurchaseAnnual))

        var receivedListOfPurchasedProducts: List<PurchasedProduct> = emptyList()

        fetcher.queryActiveProducts(
            appUserID = "appUserID",
            onSuccess = {
                receivedListOfPurchasedProducts = it
            },
            unexpectedOnError,
        )

        assertThat(receivedListOfPurchasedProducts.size).isEqualTo(2)
        val monthlyProduct =
            receivedListOfPurchasedProducts.first { it.productIdentifier == productIdentifierMonthly }
        assertPurchasedProduct(
            monthlyProduct,
            activePurchaseMonthly,
            mapOfEntitlements,
        )
        val annualProduct = receivedListOfPurchasedProducts.first { it.productIdentifier == productIdentifierAnnual }
        assertPurchasedProduct(
            annualProduct,
            activePurchaseAnnual,
            mapOfEntitlements,
        )
    }

    // region helpers
    private fun assertPurchasedProduct(
        purchasedProduct: PurchasedProduct,
        purchaseRecord: StoreTransaction,
        productIdentifierToEntitlements: Map<String, List<String>>,
    ) {
        assertThat(purchasedProduct.productIdentifier).isEqualTo(purchaseRecord.productIds[0])
        assertThat(purchasedProduct.storeTransaction).isEqualTo(purchaseRecord)
        assertThat(purchasedProduct.entitlements.size).isEqualTo(productIdentifierToEntitlements[purchasedProduct.productIdentifier]!!.size)
        assertThat(purchasedProduct.entitlements).containsAll(productIdentifierToEntitlements[purchasedProduct.productIdentifier])
        val expiresDate = testDatePlusOneDay
        assertThat(purchasedProduct.expiresDate)
            .withFailMessage("Expires date should be $expiresDate, but it was ${purchasedProduct.expiresDate}")
            .isEqualTo(expiresDate)
    }

    private fun mockActivePurchases(activePurchases: List<StoreTransaction>) {
        every {
            billing.queryPurchases(
                appUserID,
                onSuccess = captureLambda(),
                onError = any(),
            )
        } answers {
            val map = activePurchases.associateBy { it.purchaseToken.sha1() }
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured.invoke(map)
        }
    }

    private fun mockEntitlementMapping(
        productIdentifierToEntitlements: Map<String, List<String>>,
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
