package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.google.originalGooglePurchase
import com.revenuecat.purchases.models.StoreTransaction

interface PurchaseCallback : PurchaseErrorListener {
    /**
     * Will be called after the purchase has completed
     * @param storeTransaction StoreTransaction object for the purchased product.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo)
}

fun MakePurchaseListener.toPurchaseCallback(): PurchaseCallback {
    return object : PurchaseCallback {
        override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
            storeTransaction.originalGooglePurchase?.let {
                this@toPurchaseCallback.onCompleted(it, customerInfo)
            } ?: throw IllegalArgumentException("Couldn't find original Google purchase")
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toPurchaseCallback.onError(error, userCancelled)
        }
    }
}
