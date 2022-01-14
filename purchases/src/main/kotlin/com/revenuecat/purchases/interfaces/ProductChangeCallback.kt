//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.google.originalGooglePurchase
import com.revenuecat.purchases.models.StoreTransaction

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
interface ProductChangeCallback : PurchaseErrorCallback {
    /**
     * Will be called after the product change has been completed
     * @param storeTransaction StoreTransaction object for the purchased product.
     * Will be null if the change is deferred.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo)
}

@Deprecated("Deprecated in favor of ProductChangeCallback. This helper will be removed in a future release.")
fun ProductChangeListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeCallback {
        override fun onCompleted(storeTransaction: StoreTransaction?, customerInfo: CustomerInfo) {
            if (storeTransaction == null) {
                this@toProductChangeCallback.onCompleted(null, PurchaserInfo(customerInfo))
            } else {
                storeTransaction.originalGooglePurchase?.let {
                    this@toProductChangeCallback.onCompleted(it, PurchaserInfo(customerInfo))
                } ?: throw IllegalArgumentException("Couldn't find original Google purchase")
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toProductChangeCallback.onError(error, userCancelled)
        }
    }
}
