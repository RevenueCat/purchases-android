package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.PurchasesError

interface PurchaseErrorCallback {
    /**
     * Will be called after the product change has completed with error
     * @param error A [PurchasesError] containing the reason for the failure when making the product change
     * @param userCancelled A boolean indicating if the user cancelled the purchase. In that case the error will also be
     * [PurchasesErrorCode.PurchaseCancelledError]
     */
    fun onError(error: PurchasesError, userCancelled: Boolean)
}
