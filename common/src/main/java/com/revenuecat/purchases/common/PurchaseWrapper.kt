package com.revenuecat.purchases.common

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.json.JSONObject

// TODO: replace with PurchaseDetails
interface PurchaseWrapper {
    val type: ProductType
    val purchaseToken: String
    val purchaseTime: Long
    val sku: String
    val presentedOfferingIdentifier: String?
    val purchaseState: RevenueCatPurchaseState
    val storeUserID: String?
    val orderId: String
    val isAutoRenewing: Boolean?
    val signature: String?
    /*
     * null for PurchaseHistoryRecord
     */
    val originalJson: JSONObject?
}

fun PurchaseWrapper.toPurchaseDetails(): PurchaseDetails {
    return PurchaseDetails(
        orderId = this.orderId,
        sku = this.sku,
        type = this.type,
        purchaseTime = this.purchaseTime,
        purchaseToken = this.purchaseToken,
        purchaseState = this.purchaseState,
        isAutoRenewing = this.isAutoRenewing,
        signature = this.signature,
        originalJson = this.originalJson ?: throw IllegalArgumentException("")
    )
}
