package com.revenuecat.purchases

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

data class PurchaseWrapper(
    val containedPurchase: Purchase,
    @BillingClient.SkuType val type: String?
) {
    val isConsumable: Boolean = type == BillingClient.SkuType.INAPP
    val purchaseToken: String = containedPurchase.purchaseToken
    val purchaseTime: Long = containedPurchase.purchaseTime
    val sku: String = containedPurchase.sku
}