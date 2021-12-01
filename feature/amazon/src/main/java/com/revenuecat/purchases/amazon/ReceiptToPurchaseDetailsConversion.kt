package com.revenuecat.purchases.amazon

import com.amazon.device.iap.model.Receipt
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PaymentTransaction
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.RevenueCatPurchaseState

fun Receipt.toRevenueCatPurchaseDetails(
    sku: String,
    presentedOfferingIdentifier: String?,
    purchaseState: RevenueCatPurchaseState,
    storeUserID: String?
): PaymentTransaction {
    val type = this.productType.toRevenueCatProductType()
    return PaymentTransaction(
        orderId = null,
        skus = listOf(sku),
        type = type,
        purchaseTime = this.purchaseDate.time,
        purchaseToken = this.receiptId,
        purchaseState = purchaseState,
        isAutoRenewing = if (type == ProductType.SUBS) !this.isCanceled else false,
        signature = null,
        originalJson = this.toJSON(),
        presentedOfferingIdentifier = presentedOfferingIdentifier,
        storeUserID = storeUserID,
        purchaseType = PurchaseType.AMAZON_PURCHASE
    )
}
