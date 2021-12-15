//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.originalGooglePurchase
import com.revenuecat.purchases.models.PaymentTransaction

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
internal interface ProductChangeCallback : PurchaseErrorListener {
    /**
     * Will be called after the product change has been completed
     * @param paymentTransaction PaymentTransaction object for the purchased product.
     * Will be null if the change is deferred.
     * @param customerInfo Updated [CustomerInfo].
     */
    fun onCompleted(paymentTransaction: PaymentTransaction?, customerInfo: CustomerInfo)
}

internal fun ProductChangeListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeCallback {
        override fun onCompleted(paymentTransaction: PaymentTransaction?, customerInfo: CustomerInfo) {
            if (paymentTransaction == null) {
                this@toProductChangeCallback.onCompleted(null, customerInfo)
            } else {
                paymentTransaction.originalGooglePurchase?.let {
                    this@toProductChangeCallback.onCompleted(it, customerInfo)
                } ?: throw IllegalArgumentException("Couldn't find original Google purchase")
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toProductChangeCallback.onError(error, userCancelled)
        }
    }
}

internal fun MakePurchaseListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeListener {
        override fun onCompleted(purchase: Purchase?, customerInfo: CustomerInfo) {
            if (purchase == null) {
                this@toProductChangeCallback.onError(
                    PurchasesError(
                        PurchasesErrorCode.PaymentPendingError,
                        "The product change has been deferred."
                    ), false
                )
            } else {
                this@toProductChangeCallback.onCompleted(purchase, customerInfo)
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toProductChangeCallback.onError(error, userCancelled)
        }
    }.toProductChangeCallback()
}
