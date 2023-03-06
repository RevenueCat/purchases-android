package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings

interface PurchaseCallback : PurchaseErrorCallback {
    /**
     * Will be called after the purchase has completed
     * @param storeTransaction StoreTransaction object for the purchased product.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo)
}

// TODO rename, update docs
interface NewPurchaseCallback : PurchaseErrorCallback {
    /**
     * Will be called after the purchase has completed
     * @param storeTransaction StoreTransaction object for the purchased product. Null for deferred purchases.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo)
}

@Deprecated("Deprecated in favor of NewPurchaseCallback. This helper will be removed in a future release.")
fun PurchaseCallback.toNewPurchaseCallback(): NewPurchaseCallback {
    return object : NewPurchaseCallback {
        override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {
            storeTransaction?.let {
                this@toNewPurchaseCallback.onCompleted(it, customerInfo)
            } ?: run {
                val nullTransactionError = PurchasesError(
                    PurchasesErrorCode.StoreProblemError,
                    PurchaseStrings.NULL_TRANSACTION_ON_PURCHASE_ERROR
                )
                this@toNewPurchaseCallback.onError(nullTransactionError, false)
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toNewPurchaseCallback.onError(error, userCancelled)
        }
    }
}
