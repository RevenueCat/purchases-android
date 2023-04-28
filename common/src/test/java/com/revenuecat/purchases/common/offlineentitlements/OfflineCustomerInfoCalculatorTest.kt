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
import com.revenuecat.purchases.utils.stubStoreTransactionFromPurchaseHistoryRecord
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfflineCustomerInfoCalculatorTest {
    private val dateInThePast = 1.hours.ago()
    private val dateInTheFuture = 1.days.fromNow()
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
                get() = dateInThePast
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
        val purchasedProduct = mockPurchasedProducts().first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(
            setOf("${purchasedProduct.productIdentifier}:${purchasedProduct.basePlanIdentifier}")
        )
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
    }

    @Test
    fun `raw data`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockPurchasedProducts().first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
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
        val purchasedProduct = mockPurchasedProducts(
            entitlementMap = ProductEntitlementMapping(listOf(
                ProductEntitlementMapping.Mapping(productIdentifier, listOf(entitlementID, secondEntitlementID), "p1m"),
            )),
            expirationDates = mapOf(productIdentifier to dateInTheFuture)
        ).first()

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(
            setOf("${purchasedProduct.productIdentifier}:${purchasedProduct.basePlanIdentifier}")
        )
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(2)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, purchasedProduct)
    }

    @Test
    fun `product with different entitlement per base plan`() {
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val productIdentifier = "prod_1"
        val basePlan = "p1m"
        val nonBackwardsCompatibleBasePlan = "not_bw"

        // Made a purchase for p1m and then a purchase for not_bw after that one expired
        // You can't have an active purchase of the same product different base plans at the same time
        val oneHourAgo = 1.hours.ago()
        val twoHoursAgo = 2.hours.ago()
        val p1mProduct = PurchasedProduct(
            productIdentifier,
            stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = twoHoursAgo.time,
            ),
            false,
            listOf(entitlementID),
            expiresDate = oneHourAgo,
            basePlan
        )

        val notBwProduct = PurchasedProduct(
            productIdentifier,
            stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = oneHourAgo.time,
            ),
            true,
            listOf(secondEntitlementID),
            dateInTheFuture,
            nonBackwardsCompatibleBasePlan
        )

        every {
            purchasedProductsFetcher.queryPurchasedProducts(
                appUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(listOf(p1mProduct, notBwProduct))
        }

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions!!.size).isEqualTo(1)
        assertThat(receivedCustomerInfo?.activeSubscriptions).contains("prod_1:not_bw")
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(2)

        verifyEntitlement(receivedCustomerInfo, entitlementID, p1mProduct, expirationDate = oneHourAgo,
            purchaseDate = twoHoursAgo)
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, notBwProduct, purchaseDate = oneHourAgo)
    }

    @Test
    fun `multiple products`() {
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val thirdEntitlementID = "pro_3"

        val purchasedProducts = mockPurchasedProducts(
            entitlementMap = ProductEntitlementMapping(
                listOf(
                    ProductEntitlementMapping.Mapping(
                        "prod_1",
                        listOf(entitlementID),
                        "p1m"
                    ),
                    ProductEntitlementMapping.Mapping(
                        "prod_2",
                        listOf(secondEntitlementID, thirdEntitlementID),
                        "p1m"
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to dateInTheFuture,
                "prod_2" to dateInTheFuture
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
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
        val purchasedProducts = mockPurchasedProducts(
            entitlementMap = ProductEntitlementMapping(
                listOf(
                    ProductEntitlementMapping.Mapping(
                        "prod_1",
                        listOf(entitlementID),
                        "p1m"
                    ),
                    ProductEntitlementMapping.Mapping(
                        "prod_2",
                        listOf(entitlementID),
                        "p1m"
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to twoDaysFromNow,
                "prod_2" to dateInTheFuture
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
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

        val purchasedProducts = mockPurchasedProducts(
            entitlementMap = ProductEntitlementMapping(
                listOf(
                    ProductEntitlementMapping.Mapping(
                        "prod_1",
                        listOf(entitlementID),
                        "p1m"
                    ),
                    ProductEntitlementMapping.Mapping(
                        "prod_2",
                        listOf(entitlementID),
                        "p1m"
                    )
                )
            ),
            expirationDates = mapOf(
                "prod_1" to null,
                "prod_2" to dateInTheFuture
            )
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
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
        val purchasedProducts = mockPurchasedProducts(
            entitlementMap = ProductEntitlementMapping(listOf(ProductEntitlementMapping.Mapping(
                productIdentifier,
                listOf(entitlementID),
                null
            ))),
            expirationDates = mapOf(productIdentifier to null)
        )

        var receivedCustomerInfo: CustomerInfo? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("consumable").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "consumable" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct, null)
    }

    @Test
    fun `error is triggered when fetching products fails`() {
        every {
            purchasedProductsFetcher.queryPurchasedProducts(
                appUserID,
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.StoreProblemError))
        }

        var receivedError: PurchasesError? = null
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserID,
            { fail("Should've failed") },
            { receivedError = it }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    // region helpers
    private fun verifyEntitlement(
        receivedCustomerInfo: CustomerInfo?,
        entitlementID: String,
        purchasedProduct: PurchasedProduct,
        expirationDate: Date? = dateInTheFuture,
        purchaseDate: Date? = dateInThePast
    ) {
        val receivedEntitlement = receivedCustomerInfo?.entitlements?.get(entitlementID)
        assertThat(receivedEntitlement?.isActive).isTrue
        assertThat(receivedEntitlement?.identifier).isEqualTo(entitlementID)
        assertThat(receivedEntitlement?.productIdentifier).isEqualTo(purchasedProduct.productIdentifier)
        assertThat(receivedEntitlement?.billingIssueDetectedAt).isNull()
        assertThat(receivedEntitlement?.expirationDate).isEqualTo(expirationDate)
        assertThat(receivedEntitlement?.isSandbox).isFalse
        assertThat(receivedEntitlement?.originalPurchaseDate).isEqualTo(purchaseDate)
        assertThat(receivedEntitlement?.latestPurchaseDate).isEqualTo(purchaseDate)
        assertThat(receivedEntitlement?.ownershipType).isEqualTo(OwnershipType.UNKNOWN)
        assertThat(receivedEntitlement?.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(receivedEntitlement?.store).isEqualTo(Store.PLAY_STORE)
        assertThat(receivedEntitlement?.unsubscribeDetectedAt).isNull()
    }

    private fun mockPurchasedProducts(
        entitlementMap: ProductEntitlementMapping = ProductEntitlementMapping(listOf(
            ProductEntitlementMapping.Mapping("product_1", listOf("pro_1"), "p1m"),
        )),
        expirationDates: Map<String, Date?> = mapOf("product_1" to dateInTheFuture)
    ): List<PurchasedProduct> {
        val products = entitlementMap.mappings.map { (productIdentifier, entitlements, basePlanIdentifier) ->
            val expiresDate = expirationDates[productIdentifier]
            val storeTransaction = stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = dateInThePast.time
            )
            PurchasedProduct(
                productIdentifier,
                null,
                storeTransaction,
                true,
                entitlements,
                expiresDate,
                basePlanIdentifier
            )
        }

        every {
            purchasedProductsFetcher.queryPurchasedProducts(
                appUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<PurchasedProduct>) -> Unit>().captured.invoke(products)
        }
        return products
    }
    // endregion
}
