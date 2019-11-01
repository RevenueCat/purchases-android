package com.revenuecat.purchases

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord

data class PurchaseWrapper(
    val isConsumable: Boolean,
    val purchaseToken: String,
    val purchaseTime: Long,
    val sku: String,
    val containedPurchase: Purchase?,
    @BillingClient.SkuType val type: String?,
    val presentedOfferingIdentifier: String? = null
) {
    constructor(purchaseHistoryRecord: PurchaseHistoryRecord, @BillingClient.SkuType type: String?) : this(
        isConsumable = type == BillingClient.SkuType.INAPP,
        purchaseToken = purchaseHistoryRecord.purchaseToken,
        purchaseTime = purchaseHistoryRecord.purchaseTime,
        sku = purchaseHistoryRecord.sku,
        containedPurchase = null,
        type = type
    )

    constructor(purchase: Purchase, @BillingClient.SkuType type: String?, presentedOfferingIdentifier: String?) : this(
        isConsumable = type == BillingClient.SkuType.INAPP,
        purchaseToken = purchase.purchaseToken,
        purchaseTime = purchase.purchaseTime,
        sku = purchase.sku,
        containedPurchase = purchase,
        type = type,
        presentedOfferingIdentifier = presentedOfferingIdentifier
    )
}