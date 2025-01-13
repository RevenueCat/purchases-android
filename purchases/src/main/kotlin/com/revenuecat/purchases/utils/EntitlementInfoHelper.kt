package com.revenuecat.purchases.utils

import com.revenuecat.purchases.Store
import java.util.Date

internal class EntitlementInfoHelper private constructor() {

    companion object {

        fun getWillRenew(
            store: Store,
            expirationDate: Date?,
            unsubscribeDetectedAt: Date?,
            billingIssueDetectedAt: Date?,
        ): Boolean {
            val isPromo = store == Store.PROMOTIONAL
            val isLifetime = expirationDate == null
            val hasUnsubscribed = unsubscribeDetectedAt != null
            val hasBillingIssues = billingIssueDetectedAt != null
            return !(isPromo || isLifetime || hasUnsubscribed || hasBillingIssues)
        }
    }
}
