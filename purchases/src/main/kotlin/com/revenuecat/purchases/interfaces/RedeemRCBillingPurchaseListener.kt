package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI

/**
 * Interface to handle the redemption of a RevenueCat Billing purchase.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
interface RedeemRCBillingPurchaseListener {
    /**
     * Result of the redemption of a RevenueCat Billing purchase.
     */
    enum class RedeemResult {
        SUCCESS,
        ERROR,
    }

    /**
     * Interface to handle the result of the redemption of a RevenueCat Billing purchase.
     */
    interface ResultListener {
        /**
         * Method called with the result of the redemption of a RevenueCat Billing purchase.
         */
        fun handleResult(result: RedeemResult)
    }

    /**
     * Interface to start the redemption of a RevenueCat Billing purchase.
     */
    interface RedemptionStarter {
        /**
         * Starts the redemption of a RevenueCat Billing purchase.
         */
        fun startRedemption(resultListener: ResultListener)
    }

    /**
     * Called when a RevenueCat Billing purchase redemption is detected. You need to call the given
     * [RedemptionStarter.startRedemption] in order to proceed with the actual redemption.
     */
    fun handleRCBillingPurchaseRedemption(startRedemption: RedemptionStarter)
}
