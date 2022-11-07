package com.revenuecat.purchases.models

import android.os.Parcelable

/**
 * A purchase-able entity for a subscription product.
 */
interface PurchaseOption : Parcelable {
    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    val pricingPhases: List<PricingPhase>

    /**
     * Tags defined on the base plan or offer. Empty for Amazon.
     */
    val tags: List<String>

    /**
     * True if this PurchaseOption represents a Google subscription base plan (rather than an offer).
     * Not applicable for Amazon subscriptions.
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1
}

