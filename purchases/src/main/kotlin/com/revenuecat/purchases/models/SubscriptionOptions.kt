package com.revenuecat.purchases.models

import androidx.annotation.VisibleForTesting

class SubscriptionOptions(
    private val subscriptionOptions: List<SubscriptionOption>,
) : List<SubscriptionOption> by subscriptionOptions {

    private companion object {
        const val RC_IGNORE_OFFER_TAG = "rc-ignore-offer"
        const val RC_CUSTOMER_CENTER_TAG = "rc-customer-center"
    }

    /**
     * The base plan [SubscriptionOption].
     */
    val basePlan: SubscriptionOption?
        get() = this.firstOrNull { it.isBasePlan }

    /**
     * The first [SubscriptionOption] with a free trial [PricingPhase].
     */
    val freeTrial: SubscriptionOption?
        get() = this.firstOrNull { it.freePhase != null }

    /**
     * The first [SubscriptionOption] with an intro trial [PricingPhase].
     * There can be a free trial [PricingPhase] and intro trial [PricingPhase] in the same [SubscriptionOption].
     */
    val introOffer: SubscriptionOption?
        get() = this.firstOrNull { it.introPhase != null }

    /**
     * The default [SubscriptionOption]:
     *   - Filters out offers with "rc-ignore-offer" and "rc-customer-center" tag
     *   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
     *   - Falls back to use base plan
     */
    val defaultOffer: SubscriptionOption?
        get() {
            val basePlan = this.firstOrNull { it.isBasePlan } ?: return null

            val validOffers = this
                .filter { !it.isBasePlan }
                .filter { !it.tags.contains(RC_IGNORE_OFFER_TAG) }
                .filter { !it.tags.contains(RC_CUSTOMER_CENTER_TAG) }

            return findLongestFreeTrial(validOffers) ?: findLowestNonFreeOffer(validOffers) ?: basePlan
        }

    /**
     * Finds all [SubscriptionOption]s with a specific tag.
     * Note: All offers inherit base plan tags.
     */
    fun withTag(tag: String): List<SubscriptionOption> {
        return this.filter { it.tags.contains(tag) }
    }

    private fun findLongestFreeTrial(offers: List<SubscriptionOption>): SubscriptionOption? {
        return offers.mapNotNull { offer ->
            offer.freePhase?.let { pricingPhase ->
                Pair(offer, billingPeriodToDays(pricingPhase.billingPeriod))
            }
        }.maxByOrNull { it.second }?.first
    }

    private fun findLowestNonFreeOffer(offers: List<SubscriptionOption>): SubscriptionOption? {
        return offers.mapNotNull { offer ->
            offer.introPhase?.let { pricingPhase ->
                Pair(offer, pricingPhase.price.amountMicros)
            }
        }.minByOrNull { it.second }?.first
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    internal fun billingPeriodToDays(period: Period): Int {
        val days = DAYS_IN_UNIT[period.unit] ?: 0
        return period.value * days
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        (other as? SubscriptionOptions)?.let {
            if (listOf(this.subscriptionOptions) != listOf(other.subscriptionOptions)) return false

            return true
        } ?: run {
            return false
        }
    }

    override fun hashCode(): Int {
        return listOf(this.subscriptionOptions).hashCode()
    }
}

private const val DAYS_IN_DAY = 1
private const val DAYS_IN_WEEK = 7
private const val DAYS_IN_MONTH = 30
private const val DAYS_IN_YEAR = 365
private val DAYS_IN_UNIT = mapOf(
    Period.Unit.DAY to DAYS_IN_DAY,
    Period.Unit.WEEK to DAYS_IN_WEEK,
    Period.Unit.MONTH to DAYS_IN_MONTH,
    Period.Unit.YEAR to DAYS_IN_YEAR,
)
