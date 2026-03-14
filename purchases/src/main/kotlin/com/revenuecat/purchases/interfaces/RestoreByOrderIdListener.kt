package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import dev.drewhamilton.poko.Poko

/**
 * Interface to handle the result of a restore purchase by order ID operation.
 */
public fun interface RestoreByOrderIdListener {
    /**
     * Result of the restore purchase by order ID operation.
     */
    public abstract class Result internal constructor() {
        /**
         * Indicates that the purchase was successfully restored.
         */
        @Poko
        public class Success(public val customerInfo: CustomerInfo) : Result()

        /**
         * Indicates that an unknown error occurred during the operation.
         */
        @Poko
        public class Error(public val error: PurchasesError) : Result()

        /**
         * Indicates that the user hit the cool-down/rate limit.
         */
        public object RateLimitExceeded : Result()

        /**
         * Indicates that the order ID is invalid or not found.
         */
        public object OrderIdNotFound : Result()

        /**
         * Indicates that the order ID belongs to a non-consumable product, to a consumable product
         * not yet consumed, or to a virtual currency product.
         */
        public object OrderNotEligible : Result()

        /**
         * Indicates that this feature is disabled in the developer's dashboard.
         */
        public object FeatureNotEnabled : Result()

        /**
         * Indicates that the purchase related to the order ID belongs to an authenticated user
         * and cannot be transferred.
         */
        public object PurchaseBelongsToAuthenticatedUser : Result()

        /**
         * Whether the operation was successful or not.
         */
        public val isSuccess: Boolean
            get() = this is Success
    }

    /**
     * Called when a restore purchase by order ID operation finishes with the result of the operation.
     */
    public fun handleResult(result: Result)
}
