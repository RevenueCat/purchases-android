package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.RCProductType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import org.json.JSONObject

@Suppress("unused", "UNUSED_VARIABLE")
private class PaymentTransactionAPI {
    fun check(transaction: StoreTransaction) {
        with(transaction) {
            val orderId: String? = orderId
            val skus: List<String> = skus
            val type: RCProductType = type
            val purchaseTime: Long = purchaseTime
            val purchaseToken: String = purchaseToken
            val purchaseState: PurchaseState = purchaseState
            val autoRenewing: Boolean? = isAutoRenewing
            val signature: String? = signature
            val originalJson: JSONObject = originalJson
            val presentedOfferingIdentifier: String? = presentedOfferingIdentifier
            val su1: String? = storeUserID
            val purchaseType: PurchaseType = purchaseType
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
