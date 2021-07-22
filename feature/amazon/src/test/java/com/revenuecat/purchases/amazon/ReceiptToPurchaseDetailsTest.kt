package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import com.amazon.device.iap.model.ProductType as AmazonProductType

@RunWith(AndroidJUnit4::class)
class ReceiptToPurchaseDetailsTest {

    @Test
    fun `orderID is always null`() {
        val receipt = dummyReceipt()

        val purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.orderId).isNull()
    }

    @Test
    fun `passing a term skus, sku of the purchase details is not the receipt sku, but the term sku`() {
        val expectedReceiptSku = "sku"
        val expectedTermSku = "term_sku"

        val receipt = dummyReceipt(sku = expectedReceiptSku)

        val purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = expectedTermSku,
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.skus[0]).isEqualTo(expectedTermSku)
        assertThat(purchaseDetails.skus[0]).isNotEqualTo(receipt.sku)
    }

    @Test
    fun `type is correct for consumables`() {
        var receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION)

        var purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.type).isEqualTo(ProductType.SUBS)

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE)

        purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.type).isEqualTo(ProductType.INAPP)

        receipt = dummyReceipt(productType = AmazonProductType.ENTITLED)

        purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.type).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `purchase time is correct`() {
        val expectedPurchaseDate = Date()
        val receipt = dummyReceipt(purchaseDate = expectedPurchaseDate)

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.purchaseTime).isEqualTo(expectedPurchaseDate.time)
    }

    @Test
    fun `purchaseToken is the receipt ID`() {
        val receipt = dummyReceipt(receiptId = "receipt_id")

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.purchaseToken).isEqualTo(receipt.receiptId)
    }

    @Test
    fun `purchaseState is correct`() {
        val receipt = dummyReceipt()

        RevenueCatPurchaseState.values().forEach { expectedPurchaseState ->
            val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
                sku = "sku",
                presentedOfferingIdentifier = "offering",
                purchaseState = expectedPurchaseState,
                storeUserID = "store_user_id"
            )

            assertThat(purchaseDetails.purchaseState).isEqualTo(expectedPurchaseState)
        }
    }

    @Test
    fun `isAutoRenewing is false for a canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = Date())

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.isAutoRenewing).isFalse
    }

    @Test
    fun `isAutoRenewing is true for a non canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = null)

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.isAutoRenewing).isTrue
    }

    @Test
    fun `isAutoRenewing is false for non subscriptions`() {
        var receipt = dummyReceipt(productType = AmazonProductType.ENTITLED, cancelDate = null)

        var purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.isAutoRenewing).isFalse

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE, cancelDate = null)

        purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.isAutoRenewing).isFalse
    }

    @Test
    fun `signature is null`() {
        val receipt = dummyReceipt()

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.signature).isNull()
    }

    @Test
    fun `original JSON is correct`() {
        val receipt = dummyReceipt()

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        val receivedJSON = purchaseDetails.originalJson
        val expectedJSON = receipt.toJSON()
        assertThat(receivedJSON.length()).isEqualTo(expectedJSON.length())

        receivedJSON.keys().forEach {
            assertThat(receivedJSON[it]).isEqualTo(expectedJSON[it])
        }
    }

    @Test
    fun `presentedOfferingIdentifier is correct`() {
        val receipt = dummyReceipt()

        val expectedPresentedOfferingIdentifier = "offering"
        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = expectedPresentedOfferingIdentifier,
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `presentedOfferingIdentifier is correct when is null`() {
        val receipt = dummyReceipt()

        val expectedPresentedOfferingIdentifier = null
        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = expectedPresentedOfferingIdentifier,
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `storeUserID is correct`() {
        val receipt = dummyReceipt()

        val expectedStoreUserID = "store_user_id"

        val purchaseDetails: PurchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = expectedStoreUserID
        )

        assertThat(purchaseDetails.storeUserID).isEqualTo(expectedStoreUserID)
    }

    @Test
    fun `purchase type is correct`() {
        val receipt = dummyReceipt()
        val purchaseDetails = receipt.toRevenueCatPurchaseDetails(
            sku = "sku",
            presentedOfferingIdentifier = null,
            purchaseState = RevenueCatPurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(purchaseDetails.purchaseType).isEqualTo(PurchaseType.AMAZON_PURCHASE)
    }
}
