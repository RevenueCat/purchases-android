//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockPricingPhase
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubPurchaseOption
import com.revenuecat.purchases.utils.stubStoreProduct
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
            purchaseOptionId = null
        )
        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = null,
            purchaseOptions = emptyList(),
            oneTimeProductPrice = Price(
                formattedPrice = "$0.99",
                priceAmountMicros = 990000,
                currencyCode = "USD"
            )
        )

        val receiptInfo = ReceiptInfo(
            productIDs = mockStoreTransaction.productIds,
            offeringIdentifier = mockStoreTransaction.presentedOfferingIdentifier,
            storeProduct = mockStoreProduct,
            purchaseOptionId = mockStoreTransaction.purchaseOptionId
        )

        assertThat(receiptInfo.price).isEqualTo(0.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isNull()
        assertThat(receiptInfo.pricingPhases).isNull()
    }

    @Test
    fun `ReceiptInfo sets duration and pricingPhases from a StoreProduct with a subscription period and purchase options`() {
        val purchaseOptionId = "option-id"

        val mockStoreTransaction = makeMockStoreTransaction(
            purchaseState = PurchaseState.PURCHASED,
            purchaseOptionId = purchaseOptionId
        )

        val purchaseOption = stubPurchaseOption(purchaseOptionId)

        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = purchaseOption,
            purchaseOptions = listOf(purchaseOption)
        )

        val receiptInfo = ReceiptInfo(
            productIDs = mockStoreTransaction.productIds,
            offeringIdentifier = mockStoreTransaction.presentedOfferingIdentifier,
            storeProduct = mockStoreProduct,
            purchaseOptionId = mockStoreTransaction.purchaseOptionId
        )

        assertThat(receiptInfo.price).isNull()
        assertThat(receiptInfo.currency).isNull()
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

    private fun makeMockStoreTransaction(purchaseState: PurchaseState, purchaseOptionId: String?): StoreTransaction {
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
            purchaseOptionId = purchaseOptionId
        )
    }
}
