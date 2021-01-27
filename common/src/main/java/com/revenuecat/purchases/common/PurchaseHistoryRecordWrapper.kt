package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType

class PurchaseHistoryRecordWrapper(
    private val purchaseHistoryRecord: PurchaseHistoryRecord,
    override val type: ProductType
) : PurchaseWrapper {
    override val purchaseToken: String
        get() = purchaseHistoryRecord.purchaseToken
    override val purchaseTime: Long
        get() = purchaseHistoryRecord.purchaseTime
    override val sku: String
        get() = purchaseHistoryRecord.sku
    override val presentedOfferingIdentifier: String?
        get() = null
    override val purchaseState: RevenueCatPurchaseState
        get() = RevenueCatPurchaseState.UNSPECIFIED_STATE
}
