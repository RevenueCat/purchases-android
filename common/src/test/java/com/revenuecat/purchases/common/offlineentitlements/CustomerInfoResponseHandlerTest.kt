package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibm.icu.impl.Assert.fail
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
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
class CustomerInfoResponseHandlerTest {
    private val testDate = 1.hours.ago()
    private val testDatePlusOneDay = 1.days.fromNow()
    private val appUserID = "appUserID"

    private lateinit var purchasedProductsFetcher: PurchasedProductsFetcher
    private lateinit var appConfig: AppConfig

    private lateinit var testDateProvider: DateProvider

    private lateinit var customerInfoResponseHandler: CustomerInfoResponseHandler

    @Before
    fun setUp() {
        purchasedProductsFetcher = mockk()
        appConfig = mockk<AppConfig>().apply {
            every { store } returns Store.PLAY_STORE
        }
        testDateProvider = object : DateProvider {
            override val now: Date
                get() = testDate
        }
        customerInfoResponseHandler = CustomerInfoResponseHandler(purchasedProductsFetcher, testDateProvider, appConfig)
    }

    @Test
    fun `simple customer info`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockPurchasedProducts(
            expirationDate = testDatePlusOneDay
        ).first()

        var receivedCustomerInfo: CustomerInfo? = null
        customerInfoResponseHandler.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(setOf(purchasedProduct.productIdentifier))
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(1)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
    }

    @Test
    fun `raw data`() {
        val entitlementID = "pro_1"
        val purchasedProduct = mockPurchasedProducts(
            expirationDate = testDatePlusOneDay
        ).first()

        var receivedCustomerInfo: CustomerInfo? = null
        customerInfoResponseHandler.computeOfflineCustomerInfo(
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
        val purchasedProduct = mockPurchasedProducts(
            entitlementMap = mapOf("prod_1" to listOf(entitlementID, secondEntitlementID)),
            expirationDate = testDatePlusOneDay
        ).first()

        var receivedCustomerInfo: CustomerInfo? = null
        customerInfoResponseHandler.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(setOf(purchasedProduct.productIdentifier))
        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(2)

        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, purchasedProduct)
    }

    @Test
    fun `multiple products`() {
        val entitlementID = "pro_1"
        val secondEntitlementID = "pro_2"
        val thirdEntitlementID = "pro_3"

        val purchasedProducts = mockPurchasedProducts(
            entitlementMap = mapOf(
                "prod_1" to listOf(entitlementID),
                "prod_2" to listOf(secondEntitlementID, thirdEntitlementID)
            ),
            expirationDate = testDatePlusOneDay
        )

        var receivedCustomerInfo: CustomerInfo? = null
        customerInfoResponseHandler.computeOfflineCustomerInfo(
            appUserID,
            { receivedCustomerInfo = it },
            { fail("Should've succeeded") }
        )
        assertThat(receivedCustomerInfo).isNotNull

        assertThat(receivedCustomerInfo?.activeSubscriptions).isEqualTo(listOf("prod_1", "prod_2").toSet())

        assertThat(receivedCustomerInfo?.entitlements?.all?.size).isEqualTo(3)

        val purchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_1" }
        verifyEntitlement(receivedCustomerInfo, entitlementID, purchasedProduct)
        val secondPurchasedProduct = purchasedProducts.first { it.productIdentifier == "prod_2" }
        verifyEntitlement(receivedCustomerInfo, secondEntitlementID, secondPurchasedProduct)
        verifyEntitlement(receivedCustomerInfo, thirdEntitlementID, secondPurchasedProduct)
    }

    private fun verifyEntitlement(
        receivedCustomerInfo: CustomerInfo?,
        entitlementID: String,
        purchasedProduct: PurchasedProduct
    ) {
        val receivedEntitlement = receivedCustomerInfo?.entitlements?.get(entitlementID)
        assertThat(receivedEntitlement?.isActive).isTrue
        assertThat(receivedEntitlement?.identifier).isEqualTo(entitlementID)
        assertThat(receivedEntitlement?.productIdentifier).isEqualTo(purchasedProduct.productIdentifier)
        assertThat(receivedEntitlement?.billingIssueDetectedAt).isNull()
        assertThat(receivedEntitlement?.expirationDate).isEqualTo(testDatePlusOneDay)
        assertThat(receivedEntitlement?.isSandbox).isFalse
        assertThat(receivedEntitlement?.originalPurchaseDate).isEqualTo(testDate)
        assertThat(receivedEntitlement?.latestPurchaseDate).isEqualTo(testDate)
        assertThat(receivedEntitlement?.ownershipType).isEqualTo(OwnershipType.UNKNOWN)
        assertThat(receivedEntitlement?.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(receivedEntitlement?.store).isEqualTo(Store.PLAY_STORE)
        assertThat(receivedEntitlement?.unsubscribeDetectedAt).isNull()
    }

    private fun mockPurchasedProducts(
        entitlementMap: Map<String, List<String>> = mapOf("product_1" to listOf("pro_1")),
        purchaseDate: Date = testDate,
        expirationDate: Date = testDatePlusOneDay
    ): List<PurchasedProduct> {
        val products = entitlementMap.entries.map { (productIdentifier, entitlements) ->
            val storeTransaction = stubStoreTransactionFromPurchaseHistoryRecord(
                productIds = listOf(productIdentifier),
                purchaseTime = testDate.time,
            )
            PurchasedProduct(
                productIdentifier,
                storeTransaction,
                true,
                entitlements,
                expiresDate = expirationDate
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
}