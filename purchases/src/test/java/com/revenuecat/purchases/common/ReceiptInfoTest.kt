//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ReceiptInfoTest {

    private val productIdentifier = "com.myproduct"

    private val mockGooglePurchase = stubGooglePurchase(
        productIds = listOf("productIdentifier")
    )

    @Test
    fun `ReceiptInfo defaults price and currency from a INAPP StoreProduct`() {
        val mockStoreTransaction = makeMockStoreTransaction(
            purchaseState = PurchaseState.PURCHASED,
            subscriptionOptionId = null
        )
        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = null,
            subscriptionOptions = emptyList(),
            price = Price(
                formatted = "$0.99",
                amountMicros = 990000,
                currencyCode = "USD"
            )
        )

        val receiptInfo = ReceiptInfo.from(
            storeTransaction = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionsForProductIDs = null,
        )

        assertThat(receiptInfo.price).isEqualTo(0.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isNull()
        assertThat(receiptInfo.pricingPhases).isNull()
    }

    @Test
    fun `ReceiptInfo sets duration and pricingPhases from a StoreProduct with a subscription period and subscription options`() {
        val subscriptionOptionId = "option-id"

        val mockStoreTransaction = makeMockStoreTransaction(
            purchaseState = PurchaseState.PURCHASED,
            subscriptionOptionId = subscriptionOptionId
        )

        val subscriptionOption = stubSubscriptionOption(subscriptionOptionId, productIdentifier)

        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = subscriptionOption,
            subscriptionOptions = listOf(subscriptionOption)
        )

        val receiptInfo = ReceiptInfo.from(
            storeTransaction = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionsForProductIDs = null,
        )

        assertThat(receiptInfo.price).isEqualTo(4.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isEqualTo("P1M")
        assertThat(receiptInfo.pricingPhases?.size).isEqualTo(1)
    }

    @Test
    fun `ReceiptInfo allows price and currency to be set manually`() {
        val price = 0.99
        val currency = "USD"

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = price,
            currency = currency
        )

        assertThat(receiptInfo.price).isEqualTo(0.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isNull()
        assertThat(receiptInfo.pricingPhases).isNull()
    }

    @Test
    fun `platformProductIDs maintains the same order as productIDs`() {
        val product1 = "product1"
        val product2 = "product2"
        val product3 = "product3"
        val productIDs = listOf(product1, product2, product3)

        val receiptInfo = ReceiptInfo(
            productIDs = productIDs,
            platformProductIds = listOf(
                mapOf("product_id" to product1),
                mapOf("product_id" to product2),
                mapOf("product_id" to product3)
            )
        )

        val platformProductIDs = receiptInfo.platformProductIds
        assertThat(platformProductIDs).isNotNull
        assertThat(platformProductIDs!!.size).isEqualTo(3)
        assertThat(platformProductIDs[0]["product_id"]).isEqualTo(product1)
        assertThat(platformProductIDs[1]["product_id"]).isEqualTo(product2)
        assertThat(platformProductIDs[2]["product_id"]).isEqualTo(product3)
    }

    // region merge tests

    @Test
    fun `merge keeps current non-null values over cached values`() {
        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            price = 9.99,
            formattedPrice = "$9.99",
            currency = "USD",
            period = Period(1, Period.Unit.MONTH, "P1M"),
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product2"),
            price = 4.99,
            formattedPrice = "$4.99",
            currency = "EUR",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            replacementMode = GoogleReplacementMode.DEFERRED,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.productIDs).isEqualTo(listOf("product1"))
        assertThat(merged.price).isEqualTo(9.99)
        assertThat(merged.formattedPrice).isEqualTo("$9.99")
        assertThat(merged.currency).isEqualTo("USD")
        assertThat(merged.period).isEqualTo(Period(1, Period.Unit.MONTH, "P1M"))
        assertThat(merged.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_FULL_PRICE)
    }

    @Test
    fun `merge uses cached values when current values are null`() {
        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            price = null,
            formattedPrice = null,
            currency = null,
            period = null,
            replacementMode = null,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product2"),
            price = 4.99,
            formattedPrice = "$4.99",
            currency = "EUR",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            replacementMode = GoogleReplacementMode.DEFERRED,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.productIDs).isEqualTo(listOf("product1"))
        assertThat(merged.price).isEqualTo(4.99)
        assertThat(merged.formattedPrice).isEqualTo("$4.99")
        assertThat(merged.currency).isEqualTo("EUR")
        assertThat(merged.period).isEqualTo(Period(1, Period.Unit.YEAR, "P1Y"))
        assertThat(merged.replacementMode).isEqualTo(GoogleReplacementMode.DEFERRED)
    }

    @Test
    fun `merge uses cached platformProductIds when current is empty`() {
        val cachedPlatformProductIds = listOf(
            mapOf("product_id" to "product1", "base_plan_id" to "plan1")
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            platformProductIds = emptyList(),
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product1"),
            platformProductIds = cachedPlatformProductIds,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.platformProductIds).isEqualTo(cachedPlatformProductIds)
    }

    @Test
    fun `merge keeps current platformProductIds when not empty`() {
        val currentPlatformProductIds = listOf(
            mapOf("product_id" to "product1", "base_plan_id" to "plan1")
        )
        val cachedPlatformProductIds = listOf(
            mapOf("product_id" to "product2", "base_plan_id" to "plan2")
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            platformProductIds = currentPlatformProductIds,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product2"),
            platformProductIds = cachedPlatformProductIds,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.platformProductIds).isEqualTo(currentPlatformProductIds)
    }

    @Test
    fun `merge uses cached offering context when current is null`() {
        val cachedContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = PresentedOfferingContext.TargetingContext(revision = 1, ruleId = "rule1")
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = null,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = cachedContext,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.presentedOfferingContext).isEqualTo(cachedContext)
    }

    @Test
    fun `merge keeps current offering context when offering identifiers differ`() {
        val currentContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = PresentedOfferingContext.TargetingContext(revision = 1, ruleId = "rule1")
        )
        val cachedContext = PresentedOfferingContext(
            offeringIdentifier = "offering2",
            placementIdentifier = "placement2",
            targetingContext = PresentedOfferingContext.TargetingContext(revision = 2, ruleId = "rule2")
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = currentContext,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = cachedContext,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.presentedOfferingContext).isEqualTo(currentContext)
    }

    @Test
    fun `merge merges offering contexts when offering identifiers match`() {
        val currentTargetingContext = PresentedOfferingContext.TargetingContext(revision = 1, ruleId = "rule1")
        val currentContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = null
        )
        val cachedContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = null,
            targetingContext = currentTargetingContext
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = currentContext,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = cachedContext,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.presentedOfferingContext?.offeringIdentifier).isEqualTo("offering1")
        assertThat(merged.presentedOfferingContext?.placementIdentifier).isEqualTo("placement1")
        assertThat(merged.presentedOfferingContext?.targetingContext).isEqualTo(currentTargetingContext)
    }

    @Test
    fun `merge fills in missing fields from cached offering context when offering identifiers match`() {
        val cachedTargetingContext = PresentedOfferingContext.TargetingContext(revision = 1, ruleId = "rule1")
        val currentContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = null,
            targetingContext = null
        )
        val cachedContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = cachedTargetingContext
        )

        val current = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = currentContext,
        )

        val cached = ReceiptInfo(
            productIDs = listOf("product1"),
            presentedOfferingContext = cachedContext,
        )

        val merged = current.mergeWith(cached)

        assertThat(merged.presentedOfferingContext?.offeringIdentifier).isEqualTo("offering1")
        assertThat(merged.presentedOfferingContext?.placementIdentifier).isEqualTo("placement1")
        assertThat(merged.presentedOfferingContext?.targetingContext).isEqualTo(cachedTargetingContext)
    }

    // endregion merge tests

    private fun makeMockStoreTransaction(purchaseState: PurchaseState, subscriptionOptionId: String?): StoreTransaction {
        return StoreTransaction(
            orderId = mockGooglePurchase.orderId,
            productIds =  mockGooglePurchase.products,
            type = ProductType.INAPP,
            purchaseTime = mockGooglePurchase.purchaseTime,
            purchaseToken = mockGooglePurchase.purchaseToken,
            purchaseState = purchaseState,
            isAutoRenewing = mockGooglePurchase.isAutoRenewing,
            signature = mockGooglePurchase.signature,
            originalJson = JSONObject(mockGooglePurchase.originalJson),
            presentedOfferingIdentifier = null,
            storeUserID = null,
            purchaseType = PurchaseType.GOOGLE_PURCHASE,
            marketplace = null,
            subscriptionOptionId = subscriptionOptionId,
            replacementMode = null
        )
    }
}
