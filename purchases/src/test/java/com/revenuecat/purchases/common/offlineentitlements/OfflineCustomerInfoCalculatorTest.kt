package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibm.icu.impl.Assert.fail
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.ago
import com.revenuecat.purchases.common.fromNow
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.add
import com.revenuecat.purchases.utils.stubStoreTransactionFromPurchaseHistoryRecord
import com.revenuecat.purchases.utils.subtract
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfflineCustomerInfoCalculatorTest {
    private val oneHourAgo = 1.hours.ago()
    private val oneDayFromNow = 1.days.fromNow()
    private val requestDate = Date()
    private val appUserID = "appUserID"

    private lateinit var purchasedProductsFetcher: PurchasedProductsFetcher
    private lateinit var appConfig: AppConfig

    private lateinit var testDateProvider: DateProvider

    private lateinit var offlineCustomerInfoCalculator: OfflineCustomerInfoCalculator

    @Before
    fun setUp() {
        purchasedProductsFetcher = mockk()
        appConfig = mockk<AppConfig>().apply {
            every { store } returns Store.PLAY_STORE
        }
        testDateProvider = object : DateProvider {
            override val now: Date
                get() = requestDate
        }
        offlineCustomerInfoCalculator = OfflineCustomerInfoCalculator(
            purchasedProductsFetcher,
            appConfig,
            testDateProvider
        )
    }

    @Test
    fun `simple customer info`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockActiveProducts().first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(
            setOf("${purchasedProduct.productIdentifier}:${purchasedProduct.basePlanId}")
        )
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
    }

    @Test
    fun `raw data`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockActiveProducts().first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.rawData).isNotNull
        val subscriberRawData = receivedCustomerInfo?.rawData?.get("subscriber") as? JSONObject
        assertThat(subscriberRawData).isNotNull

        val entitlementsJsonObject = subscriberRawData?.get("entitlements") as? JSONObject
        assertThat(entitlementsJsonObject).isNotNull
        assertThat(entitlementsJsonObject!!.length()).isEqualTo(1)

        val subscriptionsJsonObject = subscriberRawData.get("subscriptions") as? JSONObject
        assertThat(subscriptionsJsonObject).isNotNull
        assertThat(subscriptionsJsonObject!!.length()).isEqualTo(1)

        val receivedEntitlement = receivedCustomerInfo?.entitlements?.get(entitlementID)
        assertThat(receivedEntitlement!!.rawData).isNotNull
        assertThat(receivedEntitlement.rawData["product_identifier"]).isEqualTo(purchasedProduct.productIdentifier)
    }

    @Test
    fun `product with two entitlements`() {
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val productIdentifier = "prod_1"
        val purchasedProduct = mockActiveProducts(
            entitlementMap = ProductEntitlementMapping(
                mapOf(
                    productIdentifier to ProductEntitlementMapping.Mapping(
                        productIdentifier,
                        "p1m",
                        listOf(entitlementID, secondEntitlementID)
                    )
                ),
            ),
            expirationDates = mapOf(productIdentifier to oneDayFromNow)
        ).first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(
            setOf("${purchasedProduct.productIdentifier}:${purchasedProduct.basePlanId}")
        )
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(2)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, purchasedProduct)
    }

    @Test
    fun `product with different entitlement per base plan`() {
        // Due to an issue with the way the backend returns the data, we are going to be using
        // the wrong original_purchase_date for the first entitlement
        // See https://github.com/RevenueCat/purchases-android/pull/970
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val productIdentifier = "prod_1"
        val basePlan = "p1m"
        val nonBackwardsCompatibleBasePlan = "not_bw"

        // Made a purchase for p1m and then a purchase for not_bw after that one expired
        // You can't have an active purchase of the same product different base plans at the same time
        val twoHoursAgo = oneHourAgo.subtract(1.hours)
        val p1mProduct = PurchasedProduct(
            productIdentifier,
            basePlan,
            stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = twoHoursAgo.time,
            ),
            listOf(entitlementID),
            expiresDate = oneHourAgo
        )

        val notBwProductPurchaseDate = oneHourAgo.add(1.seconds)
        val notBwProduct = PurchasedProduct(
            productIdentifier,
            nonBackwardsCompatibleBasePlan,
            stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = notBwProductPurchaseDate.time,
            ),
            listOf(secondEntitlementID),
            expiresDate = oneDayFromNow
        )

        every {
            purchasedProductsFetcher.queryActiveProducts(
                appUserID = appUserID,
                appInBackground = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(listOf(p1mProduct, notBwProduct))
        }

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions!!.size).isEqualTo(1)
        assertThat(receivedCustomerInfo?.activeSubscriptions).contains("prod_1:not_bw")
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(2)


        // This entitlement will have the original purchase date of the second purchase
        // instead of the first one until the backend starts returning original purchase date per entitlement
        verifyEntitlement(
            receivedCustomerInfo,
            entitlementID,
            p1mProduct,
            expirationDate = oneHourAgo,
            purchaseDate = twoHoursAgo,
            originalPurchaseDate = notBwProductPurchaseDate,
            isActive = false)
        verifyEntitlement(
            receivedCustomerInfo,
            secondEntitlementID,
            notBwProduct,
            expirationDate = oneDayFromNow,
            purchaseDate = notBwProductPurchaseDate
        )
    }

    @Test
    fun `multiple products`() {
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val thirdEntitlementID = "pro_3"

        val purchasedProducts = mockActiveProducts(
            entitlementMap = ProductEntitlementMapping(
                mapOf(
                    "prod_1" to ProductEntitlementMapping.Mapping(
                        "prod_1",
                        "p1m",
                        listOf(entitlementID)
                    ),
                    "prod_2" to ProductEntitlementMapping.Mapping(
                        "prod_2",
                        "p1m",
                        listOf(secondEntitlementID, thirdEntitlementID)
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to oneDayFromNow,
                "prod_2" to oneDayFromNow
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("prod_1:p1m", "prod_2:p1m").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(3)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_1" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
        val secondPurchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_2" }
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, secondPurchasedProduct)
        verifyEntitlement(receivedCustomerInfo, thirdEntitlementID, secondPurchasedProduct)
    }

    @Test
    fun `two products with overlapping entitlements prioritizes longest expiration`() {
        val entitlementID = "pro_1"

        val twoDaysFromNow = 2.days.fromNow()
        val purchasedProducts = mockActiveProducts(
            entitlementMap = ProductEntitlementMapping(
                mapOf(
                    "prod_1" to ProductEntitlementMapping.Mapping(
                        "prod_1",
                        "p1m",
                        listOf(entitlementID)
                    ),
                    "prod_2" to ProductEntitlementMapping.Mapping(
                        "prod_2",
                        "p1m",
                        listOf(entitlementID)
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to twoDaysFromNow,
                "prod_2" to oneDayFromNow
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("prod_1:p1m", "prod_2:p1m").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_1" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct, twoDaysFromNow)
    }

    @Test
    fun `two products with overlapping entitlements prioritizes the one with no expiration`() {
        val entitlementID = "pro_1"

        val purchasedProducts = mockActiveProducts(
            entitlementMap = ProductEntitlementMapping(
                mapOf(
                    "prod_1" to ProductEntitlementMapping.Mapping(
                        "prod_1",
                        "p1m",
                        listOf(entitlementID)
                    ),
                    "prod_2" to ProductEntitlementMapping.Mapping(
                        "prod_2",
                        "p1m",
                        listOf(entitlementID)
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to null,
                "prod_2" to oneDayFromNow
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("prod_1:p1m", "prod_2:p1m").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_1" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct, null)
    }

    @Test
    fun `in app product also grants entitlement`() {
        val entitlementID = "pro_1"

        val productIdentifier = "consumable"
        val purchasedProducts = mockActiveProducts(
            entitlementMap = ProductEntitlementMapping(
                mapOf(
                    productIdentifier to ProductEntitlementMapping.Mapping(
                        productIdentifier,
                        null,
                        listOf(entitlementID)
                    )
                )
            ),
            expirationDates = mapOf(productIdentifier to null)
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("consumable").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "consumable" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct, null)
    }

    @Test
    fun `error is triggered if active inapp purchase exists`() {
        val productId = "test-product-id"
        val storeTransaction = mockk<StoreTransaction>().apply {
            every { type } returns ProductType.INAPP
        }
        val products = listOf(
            PurchasedProduct(
                productId,
                null,
                storeTransaction,
                listOf("pro"),
                null
            )
        )

        every {
            purchasedProductsFetcher.queryActiveProducts(
                appUserID = appUserID,
                appInBackground = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(products)
        }

        var receivedError: PurchasesError? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { fail("Should've failed") },
            onError = { receivedError = it }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
    }

    @Test
    fun `error is triggered when fetching products fails`() {
        every {
            purchasedProductsFetcher.queryActiveProducts(
                appUserID = appUserID,
                appInBackground = any(),
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.StoreProblemError))
        }

        var receivedError: PurchasesError? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID = appUserID,
            appInBackground = false,
            onSuccess = { fail("Should've failed") },
            onError = { receivedError = it }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    // region helpers
    private fun verifyEntitlement(
        receivedCustomerInfo: CustomerInfo?,
        entitlementID: String,
        purchasedProduct: PurchasedProduct,
        expirationDate: Date? = oneDayFromNow,
        purchaseDate: Date? = oneHourAgo,
        originalPurchaseDate: Date? = purchaseDate,
        isActive: Boolean = true
    ) {
        val receivedEntitlement = receivedCustomerInfo?.entitlements?.get(entitlementID)
        assertThat(receivedEntitlement?.isActive)
            .withFailMessage("purchase date ${purchaseDate}; expiration date ${expirationDate}; requestDate ${receivedCustomerInfo?.requestDate}")
            .isEqualTo(isActive)
        assertThat(receivedEntitlement?.identifier).isEqualTo(entitlementID)
        assertThat(receivedEntitlement?.productIdentifier).isEqualTo(purchasedProduct.productIdentifier)
        assertThat(receivedEntitlement?.billingIssueDetectedAt).isNull()
        assertThat(receivedEntitlement?.expirationDate).isEqualTo(expirationDate)
        assertThat(receivedEntitlement?.isSandbox).isFalse
        assertThat(receivedEntitlement?.originalPurchaseDate).isEqualTo(originalPurchaseDate)
        assertThat(receivedEntitlement?.latestPurchaseDate).isEqualTo(purchaseDate)
        assertThat(receivedEntitlement?.ownershipType).isEqualTo(OwnershipType.UNKNOWN)
        assertThat(receivedEntitlement?.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(receivedEntitlement?.store).isEqualTo(Store.PLAY_STORE)
        assertThat(receivedEntitlement?.unsubscribeDetectedAt).isNull()
    }

    private fun mockActiveProducts(
        entitlementMap: ProductEntitlementMapping = ProductEntitlementMapping(
            mapOf(
                "product_1" to ProductEntitlementMapping.Mapping(
                    "product_1",
                    "p1m",
                    listOf("pro_1")
                )
            )
        ),
        expirationDates: Map<String, Date?> = mapOf("product_1" to oneDayFromNow)
    ): List<PurchasedProduct> {
        val products = entitlementMap.mappings.map { (productIdentifier, mapping) ->
            val expiresDate = expirationDates[productIdentifier]
            val storeTransaction = stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = oneHourAgo.time
            )
            PurchasedProduct(
                productIdentifier,
                mapping.basePlanId,
                storeTransaction,
                mapping.entitlements,
                expiresDate
            )
        }

        every {
            purchasedProductsFetcher.queryActiveProducts(
                appUserID = appUserID,
                appInBackground = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(products)
        }
        return products
    }
    // endregion
}
