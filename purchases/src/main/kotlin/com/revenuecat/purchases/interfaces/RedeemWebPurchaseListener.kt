package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to handle the redemption of a RevenueCat Web purchase.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
interface RedeemWebPurchaseListener {
    /**
     * Result of the redemption of a RevenueCat Web purchase.
     */
    sealed class Result {
        data class Success(val customerInfo: CustomerInfo) : Result()
        data class Error(val error: PurchasesError) : Result()
    }

    /**
     * Interface to handle the result of the redemption of a RevenueCat Web purchase.
     */
    interface ResultListener {
        /**
         * Method called with the result of the redemption of a RevenueCat Web purchase.
         */
        fun handleResult(result: Result)
    }

    /**
     * Interface to start the redemption of a RevenueCat Web purchase.
     */
    interface WebPurchaseRedeemer {
        /**
         * Redeems a RevenueCat Web purchase.
         */
        fun redeemWebPurchase(resultListener: ResultListener)
    }

    /**
     * Called when a RevenueCat Web purchase redemption is detected. You need to call the given
     * [WebPurchaseRedeemer.redeemWebPurchase] in order to proceed with the actual redemption.
     */
    fun handleWebPurchaseRedemption(startRedemption: WebPurchaseRedeemer)
}
