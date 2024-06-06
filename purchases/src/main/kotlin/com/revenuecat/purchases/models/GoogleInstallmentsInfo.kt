package com.revenuecat.purchases.models

/**
 * Type containing information of Google Play installment subscriptions
 */
data class GoogleInstallmentsInfo(
    /**
     * Number of payments the customer commits to in order to purchase the subscription.
     */
    override val commitmentPaymentsCount: Int,
    /**
     * After the commitment payments are made, the number of payments the user commits to upon a renewal.
     */
    override val renewalCommitmentPaymentsCount: Int,
) : InstallmentsInfo
