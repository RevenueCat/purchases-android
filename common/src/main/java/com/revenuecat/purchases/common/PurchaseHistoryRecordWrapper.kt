package com.revenuecat.purchases.common

import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.RevenueCatPurchaseState
import org.json.JSONObject

fun PurchaseHistoryRecord.toRevenueCatPurchaseDetails(
    type: ProductType
): PurchaseDetails {
    return PurchaseDetails(
        orderId = null,
        sku = this.sku,
        type = type,
        purchaseTime = this.purchaseTime,
        purchaseToken = this.purchaseToken,
        purchaseState = RevenueCatPurchaseState.UNSPECIFIED_STATE,
        isAutoRenewing = null,
        signature = this.signature,
        originalJson = JSONObject(this.originalJson),
        presentedOfferingIdentifier = null,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE
    )
}
