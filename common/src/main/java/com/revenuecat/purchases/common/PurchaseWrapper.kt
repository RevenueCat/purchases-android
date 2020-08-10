package com.revenuecat.purchases.common

import com.android.billingclient.api.Purchase

data class PurchaseWrapper(
    val isConsumable: Boolean,
    val purchaseToken: String,
    val purchaseTime: Long,
    val sku: String,
    val containedPurchase: Purchase,
    val type: PurchaseType,
    val presentedOfferingIdentifier: String? = null
) {
    constructor(
        purchase: Purchase,
        type: PurchaseType,
        presentedOfferingIdentifier: String?
    ) : this(
        isConsumable = type == PurchaseType.INAPP,
        purchaseToken = purchase.purchaseToken,
        purchaseTime = purchase.purchaseTime,
        sku = purchase.sku,
        containedPurchase = purchase,
        type = type,
        presentedOfferingIdentifier = presentedOfferingIdentifier
    )
}
