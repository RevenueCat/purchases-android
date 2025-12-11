package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.utils.parseDateFromGalaxyDateString
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import org.json.JSONObject

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Throws(IllegalArgumentException::class)
internal fun PurchaseVo.toStoreTransaction(
    productId: String,
    presentedOfferingContext: PresentedOfferingContext?,
    purchaseState: PurchaseState,
): StoreTransaction {
    val type = this.type.createRevenueCatProductTypeFromSamsungIAPTypeString()

    // throws IllegalArgumentException if the string is an invalid format
    val purchaseDate = this.purchaseDate.parseDateFromGalaxyDateString()

    return StoreTransaction(
        orderId = this.orderId,
        productIds = listOf(productId),
        type = type,
        purchaseTime = purchaseDate.time,
        purchaseToken = this.orderId,
        purchaseState = purchaseState,
        isAutoRenewing = type == ProductType.SUBS,
        signature = null,
        originalJson = JSONObject(this.jsonString),
        presentedOfferingContext = presentedOfferingContext,
        storeUserID = null,
        purchaseType = PurchaseType.GALAXY_PURCHASE,
        marketplace = null,
        subscriptionOptionId = null,
        subscriptionOptionIdForProductIDs = null,
        replacementMode = null,
    )
}
