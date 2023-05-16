package com.revenuecat.purchases.models

/**
 * A purchase-able entity for a subscription product.
 */
interface SubscriptionOption {
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
     * Tags defined on the base plan or offer. Keep in mind that offers automatically
     * inherit their base plan's tag. Empty for Amazon.
     */
    val tags: List<String>

    /**
     * The offering ID this `SubscriptionOption` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    val presentedOfferingIdentifier: String?

    /**
     * True if this SubscriptionOption represents a Google subscription base plan (rather than an offer).
     * Not applicable for Amazon subscriptions.
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1

    /**
     * The subscription period of `fullPricePhase` (after free and intro trials).
     */
    val billingPeriod: Period?
        get() = fullPricePhase?.billingPeriod

    /**
     * True if the subscription is pre-paid.
     * Not applicable for Amazon subscriptions.
     */
    val isPrepaid: Boolean
        get() =  this.fullPricePhase?.recurrenceMode == RecurrenceMode.NON_RECURRING

    /**
     * The full price [PricingPhase] of the subscription.
     * Looks for the last price phase of the SubscriptionOption.
     */
    val fullPricePhase: PricingPhase?
        get() = pricingPhases.lastOrNull()

    /**
     * The free trial [PricingPhase] of the subscription.
     * Looks for the first pricing phase of the SubscriptionOption where `amountMicros` is 0.
     * There can be a `freeTrialPhase` and an `introductoryPhase` in the same [SubscriptionOption].
     */
    val freePhase: PricingPhase?
        get() = pricingPhases.dropLast(1).firstOrNull {
            it.price.amountMicros == 0L
        }

    /**
     * The intro trial [PricingPhase] of the subscription.
     * Looks for the first pricing phase of the SubscriptionOption where `amountMicros` is greater than 0.
     * There can be a `freeTrialPhase` and an `introductoryPhase` in the same [SubscriptionOption].
     */
    val introPhase: PricingPhase?
        get() = pricingPhases.dropLast(1).firstOrNull {
            it.price.amountMicros > 0L
        }

    val purchasingData: PurchasingData
}
