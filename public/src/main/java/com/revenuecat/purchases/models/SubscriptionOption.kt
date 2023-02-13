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

    /**
     * The recurring [PricingPhase] of the subscription.
     * Looks for the last finite or infinite recurring price phase of the SubscriptionOption.
     */
    val recurringPhase: PricingPhase?
        get() = pricingPhases.last {
            it.recurrenceMode == RecurrenceMode.FINITE_RECURRING ||
                it.recurrenceMode == RecurrenceMode.INFINITE_RECURRING
        }

    /**
     * The free trial [PricingPhase] of the subscription.
     * Looks for the first pricing phase of the SubscriptionOption where `amountMicros` is 0.
     * There can be a `freeTrialPhase` and an `introductoryPhase` i the same [SubscriptionOption].
     */
    val freePhase: PricingPhase?
        get() = pricingPhases.dropLast(1).first {
            it.price.amountMicros == 0L
        }

    /**
     * The free trial [PricingPhase] of the subscription.
     * Looks for the first pricing phase of the SubscriptionOption where `amountMicros` is greater than 0.
     * There can be a `freeTrialPhase` and an `introductoryPhase` i the same [SubscriptionOption].
     */
    val introPhase: PricingPhase?
        get() = pricingPhases.dropLast(1).first {
            it.price.amountMicros > 0L
        }

    val purchasingData: PurchasingData
}
