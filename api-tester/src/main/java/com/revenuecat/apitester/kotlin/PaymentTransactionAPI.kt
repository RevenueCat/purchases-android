package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PaymentTransaction
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import org.json.JSONObject

@Suppress("unused", "UNUSED_VARIABLE")
private class PaymentTransactionAPI {
    fun check(transaction: PaymentTransaction) {
        val orderId: String? = transaction.orderId
        val skus: List<String> = transaction.skus
        val type: ProductType = transaction.type
        val purchaseTime: Long = transaction.purchaseTime
        val purchaseToken: String = transaction.purchaseToken
        val purchaseState: PurchaseState = transaction.purchaseState
        val autoRenewing: Boolean? = transaction.isAutoRenewing
        val signature: String? = transaction.signature
        val originalJson: JSONObject = transaction.originalJson
        val presentedOfferingIdentifier: String? = transaction.presentedOfferingIdentifier
        val su1: String? = transaction.storeUserID
        val purchaseType: PurchaseType = transaction.purchaseType
    }

    fun check(type: PurchaseType) {
        when (type) {
            PurchaseType.GOOGLE_PURCHASE,
            PurchaseType.GOOGLE_RESTORED_PURCHASE,
            PurchaseType.AMAZON_PURCHASE
            -> {}
        }
    }
}
