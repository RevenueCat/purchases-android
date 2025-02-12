package com.revenuecat.purchases.utils

import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import java.util.Date

internal object EntitlementInfoHelper {

    fun getWillRenew(
        store: Store,
        expirationDate: Date?,
        unsubscribeDetectedAt: Date?,
        billingIssueDetectedAt: Date?,
        periodType: PeriodType?,
    ): Boolean {
        val isPromo = store == Store.PROMOTIONAL
        val isLifetime = expirationDate == null
        val hasUnsubscribed = unsubscribeDetectedAt != null
        val hasBillingIssues = billingIssueDetectedAt != null
        val isPrepaid = periodType == PeriodType.PREPAID
        return !(isPromo || isLifetime || hasUnsubscribed || hasBillingIssues || isPrepaid)
    }
}
