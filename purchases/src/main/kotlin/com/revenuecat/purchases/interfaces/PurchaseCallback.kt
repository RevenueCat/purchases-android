package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.google.originalGooglePurchase
import com.revenuecat.purchases.models.PaymentTransaction

internal interface PurchaseCallback : PurchaseErrorListener {
    /**
     * Will be called after the purchase has completed
     * @param paymentTransaction PaymentTransaction object for the purchased product.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    fun onCompleted(paymentTransaction: PaymentTransaction, purchaserInfo: PurchaserInfo)
}

internal fun MakePurchaseListener.toPurchaseCallback(): PurchaseCallback {
    return object : PurchaseCallback {
        override fun onCompleted(paymentTransaction: PaymentTransaction, purchaserInfo: PurchaserInfo) {
            paymentTransaction.originalGooglePurchase?.let {
                this@toPurchaseCallback.onCompleted(it, purchaserInfo)
            } ?: throw IllegalArgumentException("Couldn't find original Google purchase")
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toPurchaseCallback.onError(error, userCancelled)
        }
    }
}
