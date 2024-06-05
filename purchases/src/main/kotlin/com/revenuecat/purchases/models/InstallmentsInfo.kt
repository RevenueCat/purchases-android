package com.revenuecat.purchases.models

/**
 * Type containing information of installment subscriptions. Currently only supported in Google Play.
 */
interface InstallmentsInfo {
    /**
     * Number of payments the customer commits to in order to purchase the subscription.
     */
    val commitmentPaymentsCount: Int

    /**
     * After the commitment payments are made, the number of payments the user commits to upon a renewal.
     */
    val subsequentCommitmentPaymentsCount: Int
}
