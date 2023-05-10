package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreTransactionAPI {
    fun check(transaction: StoreTransaction) {
        with(transaction) {
            val orderId: String? = orderId
            val skus: List<String> = skus
            val productIds: List<String> = productIds
            val type: ProductType = type
            val purchaseTime: Long = purchaseTime
            val purchaseToken: String = purchaseToken
            val purchaseState: PurchaseState = purchaseState
            val autoRenewing: Boolean? = isAutoRenewing
            val signature: String? = signature
            val originalJson: JSONObject = originalJson
            val presentedOfferingIdentifier: String? = presentedOfferingIdentifier
            val su1: String? = storeUserID
            val purchaseType: PurchaseType = purchaseType
            val subscriptionOptionId: String? = subscriptionOptionId
            val replacementMode: ReplacementMode? = replacementMode

            val constructedStoreTransaction = StoreTransaction(
                orderId,
                productIds,
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
                replacementMode
            )
        }
    }

    fun check(type: PurchaseType) {
        when (type) {
            PurchaseType.GOOGLE_PURCHASE,
            PurchaseType.GOOGLE_RESTORED_PURCHASE,
            PurchaseType.AMAZON_PURCHASE
            -> {}
        }.exhaustive
    }
}
