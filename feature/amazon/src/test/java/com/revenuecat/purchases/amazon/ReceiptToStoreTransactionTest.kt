package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.PurchaseState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import com.amazon.device.iap.model.ProductType as AmazonProductType

@RunWith(AndroidJUnit4::class)
class ReceiptToStoreTransactionTest {

    @Test
    fun `orderID is always null`() {
        val receipt = dummyReceipt()

        val storeTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.orderId).isNull()
    }

    @Test
    fun `passing a term skus, sku of the purchase details is not the receipt sku, but the term sku`() {
        val expectedReceiptSku = "sku"
        val expectedTermSku = "term_sku"

        val receipt = dummyReceipt(sku = expectedReceiptSku)

        val storeTransaction = receipt.toStoreTransaction(
            sku = expectedTermSku,
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.skus[0]).isEqualTo(expectedTermSku)
        assertThat(storeTransaction.skus[0]).isNotEqualTo(receipt.sku)
    }

    @Test
    fun `type is correct for consumables`() {
        var receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION)

        var storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.SUBS)

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE)

        storeTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.INAPP)

        receipt = dummyReceipt(productType = AmazonProductType.ENTITLED)

        storeTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `purchase time is correct`() {
        val expectedPurchaseDate = Date()
        val receipt = dummyReceipt(purchaseDate = expectedPurchaseDate)

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.purchaseTime).isEqualTo(expectedPurchaseDate.time)
    }

    @Test
    fun `purchaseToken is the receipt ID`() {
        val receipt = dummyReceipt(receiptId = "receipt_id")

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.purchaseToken).isEqualTo(receipt.receiptId)
    }

    @Test
    fun `purchaseState is correct`() {
        val receipt = dummyReceipt()

        PurchaseState.values().forEach { expectedPurchaseState ->
            val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
                sku = "sku",
                presentedOfferingIdentifier = "offering",
                purchaseState = expectedPurchaseState,
                storeUserID = "store_user_id"
            )

            assertThat(storeTransaction.purchaseState).isEqualTo(expectedPurchaseState)
        }
    }

    @Test
    fun `isAutoRenewing is false for a canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = Date())

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `isAutoRenewing is true for a non canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = null)

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.isAutoRenewing).isTrue
    }

    @Test
    fun `isAutoRenewing is false for non subscriptions`() {
        var receipt = dummyReceipt(productType = AmazonProductType.ENTITLED, cancelDate = null)

        var storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE, cancelDate = null)

        storeTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `signature is null`() {
        val receipt = dummyReceipt()

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.signature).isNull()
    }

    @Test
    fun `original JSON is correct`() {
        val receipt = dummyReceipt()

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        val receivedJSON = storeTransaction.originalJson
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
        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = expectedPresentedOfferingIdentifier,
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `presentedOfferingIdentifier is correct when is null`() {
        val receipt = dummyReceipt()

        val expectedPresentedOfferingIdentifier = null
        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = expectedPresentedOfferingIdentifier,
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `storeUserID is correct`() {
        val receipt = dummyReceipt()

        val expectedStoreUserID = "store_user_id"

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = "offering",
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = expectedStoreUserID
        )

        assertThat(storeTransaction.storeUserID).isEqualTo(expectedStoreUserID)
    }

    @Test
    fun `purchase type is correct`() {
        val receipt = dummyReceipt()
        val storeTransaction = receipt.toStoreTransaction(
            sku = "sku",
            presentedOfferingIdentifier = null,
            purchaseState = PurchaseState.PURCHASED,
            storeUserID = "store_user_id"
        )

        assertThat(storeTransaction.purchaseType).isEqualTo(PurchaseType.AMAZON_PURCHASE)
    }
}
