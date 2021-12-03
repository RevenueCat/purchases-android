//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.interfaces

import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.originalGooglePurchase
import com.revenuecat.purchases.models.PaymentTransaction

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
interface ProductChangeCallback : PurchaseErrorListener {
    /**
     * Will be called after the product change has been completed
     * @param paymentTransaction PaymentTransaction object for the purchased product.
     * Will be null if the change is deferred.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    fun onCompleted(paymentTransaction: PaymentTransaction?, purchaserInfo: PurchaserInfo)
}

fun ProductChangeListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeCallback {
        override fun onCompleted(paymentTransaction: PaymentTransaction?, purchaserInfo: PurchaserInfo) {
            if (paymentTransaction == null) {
                this@toProductChangeCallback.onCompleted(null, purchaserInfo)
            } else {
                paymentTransaction.originalGooglePurchase?.let {
                    this@toProductChangeCallback.onCompleted(it, purchaserInfo)
                } ?: throw IllegalArgumentException("Couldn't find original Google purchase")
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toProductChangeCallback.onError(error, userCancelled)
        }
    }
}

fun MakePurchaseListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeListener {
        override fun onCompleted(purchase: Purchase?, purchaserInfo: PurchaserInfo) {
            if (purchase == null) {
                this@toProductChangeCallback.onError(
                    PurchasesError(
                        PurchasesErrorCode.PaymentPendingError,
                        "The product change has been deferred."
                    ), false
                )
            } else {
                this@toProductChangeCallback.onCompleted(purchase, purchaserInfo)
            }
        }

        override fun onError(error: PurchasesError, userCancelled: Boolean) {
            this@toProductChangeCallback.onError(error, userCancelled)
        }
    }.toProductChangeCallback()
}
