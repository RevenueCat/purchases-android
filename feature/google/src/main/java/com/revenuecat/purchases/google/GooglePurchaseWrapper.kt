package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.PurchaseType
import org.json.JSONObject

fun Purchase.toRevenueCatPurchaseDetails(
    productType: ProductType,
    presentedOfferingIdentifier: String?
): PurchaseDetails = PurchaseDetails(
    orderId = this.orderId,
    sku = this.sku,
    type = productType,
    purchaseTime = this.purchaseTime,
    purchaseToken = this.purchaseToken,
    purchaseState = this.purchaseState.toRevenueCatPurchaseState(),
    isAutoRenewing = this.isAutoRenewing,
    signature = this.signature,
    originalJson = JSONObject(this.originalJson),
    presentedOfferingIdentifier = presentedOfferingIdentifier,
    storeUserID = null,
    purchaseType = PurchaseType.GOOGLE_PURCHASE
)

// TODO: should this be nullable or just throw
val PurchaseDetails.originalGooglePurchase: Purchase?
    get() = this.signature?.let { signature ->
        Purchase(this.originalJson.toString(), signature)
    }
