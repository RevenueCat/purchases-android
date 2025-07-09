package com.revenuecat.purchases.models

import dev.drewhamilton.poko.Poko

/**
 * Type containing information of Google Play installment subscriptions
 */
@Poko
class GoogleInstallmentsInfo(
    /**
     * Number of payments the customer commits to in order to purchase the subscription.
     */
    override val commitmentPaymentsCount: Int,
    /**
     * After the commitment payments are made, the number of payments the user commits to upon a renewal.
     */
    override val renewalCommitmentPaymentsCount: Int,
) : InstallmentsInfo
