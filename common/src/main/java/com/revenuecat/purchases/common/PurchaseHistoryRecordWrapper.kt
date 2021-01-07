package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType

data class PurchaseHistoryRecordWrapper(
    val isConsumable: Boolean,
    val purchaseToken: String,
    val purchaseTime: Long,
    val sku: String,
    val purchaseHistoryRecord: PurchaseHistoryRecord,
    val type: ProductType
) {
    constructor(
        purchaseHistoryRecord: PurchaseHistoryRecord,
        type: ProductType
    ) : this(
        isConsumable = type == ProductType.INAPP, // TODO: check this
        purchaseToken = purchaseHistoryRecord.purchaseToken,
        purchaseTime = purchaseHistoryRecord.purchaseTime,
        sku = purchaseHistoryRecord.sku,
        purchaseHistoryRecord = purchaseHistoryRecord,
        type = type
    )
}
