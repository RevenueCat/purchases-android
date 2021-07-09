package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.json.JSONObject

fun Purchase.toRevenueCatPurchaseDetails(
    productType: ProductType,
    presentedOfferingIdentifier: String?
): PurchaseDetails = PurchaseDetails(
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

val PurchaseDetails.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }

fun PurchaseHistoryRecord.toRevenueCatPurchaseDetails(
    type: ProductType
): PurchaseDetails {
    return PurchaseDetails(
        orderId = null,
        skus = this.skus,
        type = type,
        purchaseTime = this.purchaseTime,
        purchaseToken = this.purchaseToken,
        purchaseState = RevenueCatPurchaseState.UNSPECIFIED_STATE,
        isAutoRenewing = null,
        signature = this.signature,
        originalJson = JSONObject(this.originalJson),
        presentedOfferingIdentifier = null,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE
    )
}
