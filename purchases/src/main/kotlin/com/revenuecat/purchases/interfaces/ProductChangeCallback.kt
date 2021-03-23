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
import com.revenuecat.purchases.models.PurchaseDetails

/**
 * Interface to be implemented when upgrading or downgrading a purchase.
 */
interface ProductChangeCallback : PurchaseErrorListener {
    /**
     * Will be called after the product change has been completed
     * @param purchase PurchaseDetails object for the purchased product. Will be null if the change is deferred.
     * @param purchaserInfo Updated [PurchaserInfo].
     */
    fun onCompleted(purchase: PurchaseDetails?, purchaserInfo: PurchaserInfo)
}

internal fun ProductChangeListener.toProductChangeCallback(): ProductChangeCallback {
    return object : ProductChangeCallback {
        override fun onCompleted(purchase: PurchaseDetails?, purchaserInfo: PurchaserInfo) {
            if (purchase == null) {
                this@toProductChangeCallback.onCompleted(null, purchaserInfo)
            } else {
                purchase.originalGooglePurchase?.let {
                    this@toProductChangeCallback.onCompleted(it, purchaserInfo)
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
