package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

fun Purchase.toStoreTransaction(
    productType: ProductType,
    presentedOfferingIdentifier: String?,
    subscriptionOptionId: String? = null
): StoreTransaction = StoreTransaction(
    orderId = this.orderId,
    productIds = this.products,
    type = productType,
    purchaseTime = this.purchaseTime,
    purchaseToken = this.purchaseToken,
    purchaseState = this.purchaseState.toRevenueCatPurchaseState(),
    isAutoRenewing = this.isAutoRenewing,
    signature = this.signature,
    originalJson = JSONObject(this.originalJson),
    presentedOfferingIdentifier = presentedOfferingIdentifier,
    storeUserID = null,
    purchaseType = PurchaseType.GOOGLE_PURCHASE,
    marketplace = null,
    subscriptionOptionId = subscriptionOptionId
)

val StoreTransaction.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }

fun PurchaseHistoryRecord.toStoreTransaction(
    type: ProductType
): StoreTransaction {
    return StoreTransaction(
        orderId = null,
        productIds = this.products,
        type = type,
        purchaseTime = this.purchaseTime,
        purchaseToken = this.purchaseToken,
        purchaseState = PurchaseState.UNSPECIFIED_STATE,
        isAutoRenewing = null,
        signature = this.signature,
        originalJson = JSONObject(this.originalJson),
        presentedOfferingIdentifier = null,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE,
        marketplace = null,
        subscriptionOptionId = null
    )
}
