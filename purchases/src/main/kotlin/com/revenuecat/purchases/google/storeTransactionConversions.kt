package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

internal fun Purchase.toStoreTransaction(
    productType: ProductType,
    presentedOfferingIdentifier: String? = null,
    subscriptionOptionId: String? = null,
    prorationMode: GoogleProrationMode? = null,
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
    subscriptionOptionId = subscriptionOptionId,
    prorationMode = prorationMode,
)

internal fun Purchase.toStoreTransaction(purchaseContext: PurchaseContext): StoreTransaction =
    toStoreTransaction(
        purchaseContext.productType,
        purchaseContext.presentedOfferingId,
        purchaseContext.selectedSubscriptionOptionId,
        purchaseContext.prorationMode,
    )

internal val StoreTransaction.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }

internal fun PurchaseHistoryRecord.toStoreTransaction(
    type: ProductType,
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
        subscriptionOptionId = null,
        prorationMode = null,
    )
}
