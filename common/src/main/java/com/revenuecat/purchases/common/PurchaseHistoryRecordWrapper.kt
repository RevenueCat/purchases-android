package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType

class PurchaseHistoryRecordWrapper : PurchaseWrapper {

    override val type: ProductType
    override val purchaseToken: String
    override val purchaseTime: Long
    override val sku: String
    override val presentedOfferingIdentifier: String?
    override val purchaseState: RevenueCatPurchaseState
    override val storeUserID: String?

    constructor(
        type: ProductType,
        purchaseToken: String,
        purchaseTime: Long,
        sku: String,
        presentedOfferingIdentifier: String? = null,
        purchaseState: RevenueCatPurchaseState,
        storeUserID: String?
    ) {
        this.type = type
        this.purchaseToken = purchaseToken
        this.purchaseTime = purchaseTime
        this.sku = sku
        this.presentedOfferingIdentifier = presentedOfferingIdentifier
        this.purchaseState = purchaseState
        this.storeUserID = storeUserID
    }

    constructor(
        purchaseHistoryRecord: PurchaseHistoryRecord,
        type: ProductType
    ) : this(
        type = type,
        purchaseToken = purchaseHistoryRecord.purchaseToken,
        purchaseTime = purchaseHistoryRecord.purchaseTime,
        sku = purchaseHistoryRecord.sku,
        purchaseState = RevenueCatPurchaseState.UNSPECIFIED_STATE,
        storeUserID = null
    )
}
