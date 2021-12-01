package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PaymentTransaction
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.PurchaseState
import org.json.JSONObject

fun Purchase.toRevenueCatPurchaseDetails(
    productType: ProductType,
    presentedOfferingIdentifier: String?
): PaymentTransaction = PaymentTransaction(
    orderId = this.orderId,
    skus = this.skus,
    type = productType,
    purchaseTime = this.purchaseTime,
    purchaseToken = this.purchaseToken,
    purchaseState = this.purchaseState.toRevenueCatPurchaseState(),
    isAutoRenewing = this.isAutoRenewing,
    signature = this.signature,
    originalJson = JSONObject(this.originalJson),
    presentedOfferingIdentifier = presentedOfferingIdentifier,
    storeUserID = null,
    purchaseType = PurchaseType.GOOGLE_PURCHASE
)

val PaymentTransaction.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }

fun PurchaseHistoryRecord.toRevenueCatPurchaseDetails(
    type: ProductType
): PaymentTransaction {
    return PaymentTransaction(
        orderId = null,
        skus = this.skus,
        type = type,
        purchaseTime = this.purchaseTime,
        purchaseToken = this.purchaseToken,
        purchaseState = PurchaseState.UNSPECIFIED_STATE,
        isAutoRenewing = null,
        signature = this.signature,
        originalJson = JSONObject(this.originalJson),
        presentedOfferingIdentifier = null,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE
    )
}
