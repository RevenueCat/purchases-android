package com.revenuecat.purchases.models

import com.android.billingclient.api.BillingFlowParams

/**
 * Enum of possible proration modes to be passed to a Google Play purchase.
 * Ignored for Amazon purchases. <-- TODOBC5 confirm
 *
 * See https://developer.android.com/google/play/billing/subscriptions#proration for examples
 */
enum class GoogleProrationMode(@BillingFlowParams.ProrationMode val playBillingClientName: Int) {
    /**
     * New subscription benefits take effect immediately.
     * User is charged for the full price of the new subscription on the old subscription's expiration date.
     *
     * This is the default behavior.
     */
    IMMEDIATE_WITHOUT_PRORATION(BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION),

    /**
     * New subscription benefits take effect immediately.
     * Any time remaining on the old subscription is used to push out the first payment date for the new subscription.
     * User is charged the full price of new subscription once that prorated time has passed.
     *
     * The purchase will fail if this mode is used when switching between [PurchaseOption]s of the same [StoreProduct].
     */
    IMMEDIATE_WITH_TIME_PRORATION(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
}
