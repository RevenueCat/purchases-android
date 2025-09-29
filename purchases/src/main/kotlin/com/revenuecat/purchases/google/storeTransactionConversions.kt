package com.revenuecat.purchases.google

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

internal fun Purchase.toStoreTransaction(
    productType: ProductType,
    presentedOfferingContext: PresentedOfferingContext? = null,
    subscriptionOptionId: String? = null,
    subscriptionOptionIdsForProductIDs: Map<String, String>? = null,
    replacementMode: GoogleReplacementMode? = null,
): StoreTransaction = StoreTransaction(
    orderId = this.orderId,
    productIds = this.products,
    type = productType,
    purchaseTime = this.purchaseTime,
    purchaseToken = this.purchaseToken,
    purchaseState = this.purchaseState.toRevenueCatPurchaseState(),
    isAutoRenewing = this.isAutoRenewing,
    signature = this.signature,
    originalJson = JSONObject(this.originalJson),
    presentedOfferingContext = presentedOfferingContext,
    storeUserID = null,
    purchaseType = PurchaseType.GOOGLE_PURCHASE,
    marketplace = null,
    subscriptionOptionId = subscriptionOptionId,
    subscriptionOptionIdsForProductIDs = subscriptionOptionIdsForProductIDs,
    replacementMode = replacementMode,
)

internal fun Purchase.toStoreTransaction(purchaseContext: PurchaseContext): StoreTransaction =
    toStoreTransaction(
        purchaseContext.productType,
        purchaseContext.presentedOfferingContext,
        purchaseContext.selectedSubscriptionOptionId,
        purchaseContext.subscriptionOptionIdsForProductIDs,
        purchaseContext.replacementMode,
    )

internal val StoreTransaction.originalGooglePurchase: Purchase?
    get() =
        this.signature
            ?.takeIf { this.purchaseType == PurchaseType.GOOGLE_PURCHASE }
            ?.let { signature -> Purchase(this.originalJson.toString(), signature) }
