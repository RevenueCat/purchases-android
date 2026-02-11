package com.revenuecat.purchases.google.history

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

/**
 * Data class representing a purchase history record obtained through an AIDL connection in BC8.
 * This includes the purchase data and its cryptographic signature.
 */
internal data class PurchaseHistoryRecord(
    val purchaseData: PurchaseData,
    val signature: String,
    val rawJson: String,
) {
    public fun toStoreTransaction(type: ProductType): StoreTransaction {
        return StoreTransaction(
            orderId = this.purchaseData.orderId,
            productIds = listOf(this.purchaseData.productId),
            type = type,
            purchaseTime = this.purchaseData.purchaseTime,
            purchaseToken = this.purchaseData.purchaseToken,
            purchaseState = PurchaseState.UNSPECIFIED_STATE,
            isAutoRenewing = null,
            signature = this.signature,
            originalJson = JSONObject(this.rawJson),
            presentedOfferingContext = null,
            storeUserID = null,
            purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE,
            marketplace = null,
            subscriptionOptionId = null,
            replacementMode = null,
        )
    }
}
