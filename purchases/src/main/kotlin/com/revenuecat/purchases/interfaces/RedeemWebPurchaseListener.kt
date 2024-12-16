package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Interface to handle the redemption of a RevenueCat Web purchase.
 */
fun interface RedeemWebPurchaseListener {
    /**
     * Result of the redemption of a RevenueCat Web purchase.
     */
    sealed class Result {
        /**
         * Indicates that the web purchase was redeemed successfully.
         */
        data class Success(val customerInfo: CustomerInfo) : Result()

        /**
         * Indicates that an unknown error occurred during the redemption.
         */
        data class Error(val error: PurchasesError) : Result()

        /**
         * Indicates that the redemption token is invalid.
         */
        object InvalidToken : Result()

        /**
         * Indicates that the redemption token has expired. An email with a new redemption token
         * might be sent if a new one wasn't already sent recently.
         * The email where it will be sent is indicated by the [obfuscatedEmail].
         */
        data class Expired(val obfuscatedEmail: String) : Result()

        /**
         * Indicates that the redemption couldn't be performed because the purchase belongs to a different user.
         */
        object PurchaseBelongsToOtherUser : Result()

        /**
         * Whether the redemption was successful or not.
         */
        val isSuccess: Boolean
            get() = when (this) {
                is Success -> true
                is Error -> false
                InvalidToken -> false
                is Expired -> false
                PurchaseBelongsToOtherUser -> false
            }
    }

    /**
     * Called when a RevenueCat Web purchase redemption finishes with the result of the operation.
     */
    fun handleResult(result: Result)
}
