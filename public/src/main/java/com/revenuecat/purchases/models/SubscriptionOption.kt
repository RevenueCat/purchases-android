package com.revenuecat.purchases.models

import android.os.Parcelable

/**
 * A purchase-able entity for a subscription product.
 */
interface SubscriptionOption : Parcelable {
    /**
     * For Google subscriptions:
     * If this SubscriptionOption represents a base plan, this will be the basePlanId.
     * If it represents an offer, it will be {basePlanId}:{offerId}
     *
     * Not applicable for Amazon subscriptions.
     */
    val id: String

    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    val pricingPhases: List<PricingPhase>

    /**
     * Tags defined on the base plan or offer. Empty for Amazon.
     */
    val tags: List<String>

    /**
     * True if this SubscriptionOption represents a Google subscription base plan (rather than an offer).
     * Not applicable for Amazon subscriptions.
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1

    val purchasingData: PurchasingData
}