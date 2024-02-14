package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.amazon.helpers.dummyUserData
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

    private val presentedOfferingContext = PresentedOfferingContext("offering")

    @Test
    fun `orderID is always null`() {
        val receipt = dummyReceipt()

        val storeTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.orderId).isNull()
    }

    @Test
    fun `passing a term skus, sku of the purchase details is not the receipt sku, but the term sku`() {
        val expectedReceiptSku = "sku"
        val expectedTermSku = "term_sku"

        val receipt = dummyReceipt(sku = expectedReceiptSku)

        val storeTransaction = receipt.toStoreTransaction(
            productId = expectedTermSku,
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.skus[0]).isEqualTo(expectedTermSku)
        assertThat(storeTransaction.skus[0]).isNotEqualTo(receipt.sku)
    }

    @Test
    fun `type is correct for consumables`() {
        var receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION)

        var storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.SUBS)

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE)

        storeTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.INAPP)

        receipt = dummyReceipt(productType = AmazonProductType.ENTITLED)

        storeTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.type).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `purchase time is correct`() {
        val expectedPurchaseDate = Date()
        val receipt = dummyReceipt(purchaseDate = expectedPurchaseDate)

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.purchaseTime).isEqualTo(expectedPurchaseDate.time)
    }

    @Test
    fun `purchaseToken is the receipt ID`() {
        val receipt = dummyReceipt(receiptId = "receipt_id")

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.purchaseToken).isEqualTo(receipt.receiptId)
    }

    @Test
    fun `purchaseState is correct`() {
        val receipt = dummyReceipt()

        PurchaseState.values().forEach { expectedPurchaseState ->
            val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
                productId = "sku",
                presentedOfferingContext = presentedOfferingContext,
                purchaseState = expectedPurchaseState,
                userData = dummyUserData(
                    storeUserId = "store_user_id",
                    marketplace = "US"
                ),
            )

            assertThat(storeTransaction.purchaseState).isEqualTo(expectedPurchaseState)
        }
    }

    @Test
    fun `isAutoRenewing is false for a canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = Date())

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `isAutoRenewing is true for a non canceled subscription`() {
        val receipt = dummyReceipt(productType = AmazonProductType.SUBSCRIPTION, cancelDate = null)

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.isAutoRenewing).isTrue
    }

    @Test
    fun `isAutoRenewing is false for non subscriptions`() {
        var receipt = dummyReceipt(productType = AmazonProductType.ENTITLED, cancelDate = null)

        var storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse

        receipt = dummyReceipt(productType = AmazonProductType.CONSUMABLE, cancelDate = null)

        storeTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.isAutoRenewing).isFalse
    }

    @Test
    fun `signature is null`() {
        val receipt = dummyReceipt()

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.signature).isNull()
    }

    @Test
    fun `original JSON is correct`() {
        val receipt = dummyReceipt()

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
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
            productId = "sku",
            presentedOfferingContext = PresentedOfferingContext(expectedPresentedOfferingIdentifier),
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
        assertThat(storeTransaction.presentedOfferingContext?.offeringIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `presentedOfferingIdentifier is correct when is null`() {
        val receipt = dummyReceipt()

        val expectedPresentedOfferingIdentifier = null
        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = PresentedOfferingContext(expectedPresentedOfferingIdentifier),
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.presentedOfferingIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
        assertThat(storeTransaction.presentedOfferingContext?.offeringIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `storeUserID is correct`() {
        val receipt = dummyReceipt()

        val expectedStoreUserID = "store_user_id"

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = expectedStoreUserID,
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.storeUserID).isEqualTo(expectedStoreUserID)
    }

    @Test
    fun `marketplace is correct`() {
        val receipt = dummyReceipt()

        val expectedMarketplace = "US"

        val storeTransaction: StoreTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = expectedMarketplace
            )
        )

        assertThat(storeTransaction.marketplace).isEqualTo(expectedMarketplace)
    }

    @Test
    fun `purchase type is correct`() {
        val receipt = dummyReceipt()
        val storeTransaction = receipt.toStoreTransaction(
            productId = "sku",
            presentedOfferingContext = presentedOfferingContext,
            purchaseState = PurchaseState.PURCHASED,
            userData = dummyUserData(
                storeUserId = "store_user_id",
                marketplace = "US"
            )
        )

        assertThat(storeTransaction.purchaseType).isEqualTo(PurchaseType.AMAZON_PURCHASE)
    }
}
