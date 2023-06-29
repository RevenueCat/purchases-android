package com.revenuecat.purchases.amazon

import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction

internal fun Receipt.toStoreTransaction(
    productId: String,
    presentedOfferingIdentifier: String?,
    purchaseState: PurchaseState,
    userData: UserData,
): StoreTransaction {
    val type = this.productType.toRevenueCatProductType()
    return StoreTransaction(
        orderId = null,
        productIds = listOf(productId),
        type = type,
        purchaseTime = this.purchaseDate.time,
        purchaseToken = this.receiptId,
        purchaseState = purchaseState,
        isAutoRenewing = if (type == ProductType.SUBS) !this.isCanceled else false,
        signature = null,
        originalJson = this.toJSON(),
        presentedOfferingIdentifier = presentedOfferingIdentifier,
        storeUserID = userData.userId,
        purchaseType = PurchaseType.AMAZON_PURCHASE,
        marketplace = userData.marketplace,
        subscriptionOptionId = null,
        prorationMode = null,
    )
}
