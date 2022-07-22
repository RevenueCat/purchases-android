package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.RCProductType
import com.revenuecat.purchases.common.listOfSkus
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.PurchaseState
import org.json.JSONObject

fun Purchase.toStoreTransaction(
    productType: RCProductType,
    presentedOfferingIdentifier: String?
): StoreTransaction = StoreTransaction(
    orderId = this.orderId,
    skus = this.listOfSkus,
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
    marketplace = null
)

val StoreTransaction.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }

fun PurchaseHistoryRecord.toStoreTransaction(
    type: RCProductType
): StoreTransaction {
    return StoreTransaction(
        orderId = null,
        skus = this.listOfSkus,
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
        marketplace = null
    )
}
