package com.revenuecat.purchases.models

import com.android.billingclient.api.BillingFlowParams

/**
 * Enum of possible proration modes to be passed to a Google Play purchase
 */
enum class GoogleProrationMode(@BillingFlowParams.ProrationMode val playBillingClientName: Int) {
    IMMEDIATE_WITHOUT_PRORATION(BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION),
    IMMEDIATE_WITH_TIME_PRORATION(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
}
