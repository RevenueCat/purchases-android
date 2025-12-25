//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
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
