package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.json.JSONObject

class PurchaseHistoryRecordWrapper(
    override val type: ProductType,
    override val purchaseToken: String,
    override val purchaseTime: Long,
    override val sku: String,
    override val presentedOfferingIdentifier: String? = null,
    override val purchaseState: RevenueCatPurchaseState,
    override val storeUserID: String?,
    override val isAutoRenewing: Boolean?,
    override val signature: String?,
    override val orderId: String = purchaseToken,
    override val originalJson: JSONObject?
) : PurchaseWrapper {

    constructor(
        purchaseHistoryRecord: PurchaseHistoryRecord,
        type: ProductType
    ) : this(
        type = type,
        purchaseToken = purchaseHistoryRecord.purchaseToken,
        purchaseTime = purchaseHistoryRecord.purchaseTime,
        sku = purchaseHistoryRecord.sku,
        purchaseState = RevenueCatPurchaseState.UNSPECIFIED_STATE,
        storeUserID = null,
        isAutoRenewing = null,
        signature = purchaseHistoryRecord.signature,
        originalJson = null
    )
}
