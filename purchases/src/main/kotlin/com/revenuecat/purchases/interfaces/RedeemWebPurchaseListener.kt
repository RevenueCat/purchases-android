package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to handle the redemption of a RevenueCat Web purchase.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
fun interface RedeemWebPurchaseListener {
    /**
     * Result of the redemption of a RevenueCat Web purchase.
     */
    sealed class Result {
        data class Success(val customerInfo: CustomerInfo) : Result()
        data class Error(val error: PurchasesError) : Result()

        /**
         * Whether the redemption was successful or not.
         */
        val isSuccess: Boolean
            get() = when (this) {
                is Success -> true
                is Error -> false
            }
    }

    /**
     * Called when a RevenueCat Web purchase redemption finishes with the result of the operation.
     */
    fun handleResult(result: Result)
}
