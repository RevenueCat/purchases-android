package com.revenuecat.purchases

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord

internal data class PurchaseWrapper(
    val isConsumable: Boolean,
    val purchaseToken: String,
    val purchaseTime: Long,
    val sku: String,
    val containedPurchase: Purchase?,
    val type: PurchaseType,
    val presentedOfferingIdentifier: String? = null
) {
    constructor(purchaseHistoryRecord: PurchaseHistoryRecord, type: PurchaseType) : this(
        isConsumable = type == PurchaseType.SUBS,
        purchaseToken = purchaseHistoryRecord.purchaseToken,
        purchaseTime = purchaseHistoryRecord.purchaseTime,
        sku = purchaseHistoryRecord.sku,
        containedPurchase = null,
        type = type
    )

    constructor(purchase: Purchase, type: PurchaseType, presentedOfferingIdentifier: String?) : this(
        isConsumable = type == PurchaseType.INAPP,
        purchaseToken = purchase.purchaseToken,
        purchaseTime = purchase.purchaseTime,
        sku = purchase.sku,
        containedPurchase = purchase,
        type = type,
        presentedOfferingIdentifier = presentedOfferingIdentifier
    )
}