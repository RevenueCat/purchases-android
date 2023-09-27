package com.revenuecat.purchases.models

import com.android.billingclient.api.InAppMessageParams

/**
 * Enum mapping in-app message types
 */
enum class InAppMessageType(@InAppMessageParams.InAppMessageCategoryId internal val inAppMessageCategoryId: Int) {
    /**
     * In-app messages for billing issues.
     * If the user has had a payment declined, this will show a toast notification notifying them and
     * providing instructions for recovery of the subscription.
     */
    BILLING_ISSUES(InAppMessageParams.InAppMessageCategoryId.TRANSACTIONAL),
}
