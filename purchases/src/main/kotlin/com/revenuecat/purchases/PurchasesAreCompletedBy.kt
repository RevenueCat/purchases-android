package com.revenuecat.purchases

/**
 * Modes for completing the purchase process.
 */
enum class PurchasesAreCompletedBy {
    /**
     * RevenueCat will automatically acknowledge verified purchases. No action is required by you.
     */
    REVENUECAT,

    /**
     * RevenueCat will **not** automatically acknowledge any purchases. You will have to do so manually.
     *
     * **Note:** failing to acknowledge a purchase within 3 days will lead to Google Play automatically issuing a
     * refund to the user.
     *
     * For more info, see
     * [revenuecat.com](https://www.revenuecat.com/docs/migrating-to-revenuecat/sdk-or-not/finishing-transactions) and
     * [developer.android.com](https://developer.android.com/google/play/billing/integrate#process).
     */
    MY_APP,
}

/**
 * Whether the Purchases SDK should finish transactions.
 */
internal val PurchasesAreCompletedBy.finishTransactions: Boolean
    get() = when (this) {
        PurchasesAreCompletedBy.REVENUECAT -> true
        PurchasesAreCompletedBy.MY_APP -> false
    }
