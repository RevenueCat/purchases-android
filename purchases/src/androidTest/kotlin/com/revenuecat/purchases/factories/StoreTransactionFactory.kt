package com.revenuecat.purchases.factories

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

object StoreTransactionFactory {
    @Suppress("LongParameterList")
    fun createStoreTransaction(
        orderId: String? = "test-order-id",
        skus: List<String> = listOf(Constants.productIdToPurchase),
        type: ProductType = ProductType.SUBS,
        purchaseTime: Long = 1679575266000L, // Thursday, March 23, 2023 12:41:06 PM GMT
        purchaseToken: String = Constants.googlePurchaseToken,
        purchaseState: PurchaseState = PurchaseState.PURCHASED,
        isAutoRenewing: Boolean? = true,
        signature: String? = "test-signature",
        originalJson: JSONObject = JSONObject(),
        presentedOfferingIdentifier: String? = null,
        storeUserID: String? = null,
        purchaseType: PurchaseType = PurchaseType.GOOGLE_PURCHASE,
        marketplace: String? = null,
        subscriptionOptionId: String? = Constants.basePlanIdToPurchase,
        replacementMode: ReplacementMode? = null,
    ): StoreTransaction {
        return StoreTransaction(
            orderId,
            skus,
            type,
            purchaseTime,
            purchaseToken,
            purchaseState,
            isAutoRenewing,
            signature,
            originalJson,
            presentedOfferingIdentifier,
            storeUserID,
            purchaseType,
            marketplace,
            subscriptionOptionId,
            replacementMode,
        )
    }
}
