package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.json.JSONObject

// TODO: replace with PurchaseDetails
class GooglePurchaseWrapper(
    val containedPurchase: Purchase,
    override val type: ProductType,
    override val presentedOfferingIdentifier: String? = null
) : PurchaseWrapper {
    override val purchaseToken: String
        get() = containedPurchase.purchaseToken
    override val purchaseTime: Long
        get() = containedPurchase.purchaseTime
    override val sku: String
        get() = containedPurchase.sku
    override val purchaseState: RevenueCatPurchaseState
        get() = containedPurchase.purchaseState.toRevenueCatPurchaseType()
    override val storeUserID: String?
        get() = null
    override val orderId: String
        get() = containedPurchase.orderId
    override val isAutoRenewing: Boolean?
        get() = containedPurchase.isAutoRenewing
    override val signature: String?
        get() = containedPurchase.signature
    override val originalJson: JSONObject?
        get() = JSONObject(containedPurchase.originalJson)
}
