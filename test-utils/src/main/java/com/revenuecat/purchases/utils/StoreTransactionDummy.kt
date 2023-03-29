package com.revenuecat.purchases.utils

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject
import java.util.Date

fun dummyGoogleRestoredStoreTransaction(
    purchaseDate: Date,
): StoreTransaction {
    return StoreTransaction(
        orderId = "dummyOrderId",
        skus = listOf("product1", "product2"),
        type = ProductType.SUBS,
        purchaseTime = purchaseDate.time,
        purchaseToken = "dummyPurchaseToken",
        purchaseState = PurchaseState.PURCHASED,
        isAutoRenewing = null,
        signature = "dummySignature",
        originalJson = JSONObject("{}"), // TODO: do we need to fill this?
        presentedOfferingIdentifier = null,
        storeUserID = null,
        purchaseType = PurchaseType.GOOGLE_RESTORED_PURCHASE,
        marketplace = null
    )
}
