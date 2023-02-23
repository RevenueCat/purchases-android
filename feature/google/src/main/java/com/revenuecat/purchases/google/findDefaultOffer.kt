package com.revenuecat.purchases.google

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase

fun List<GoogleSubscriptionOption>.findDefaultOffer(): GoogleSubscriptionOption? {
    val basePlan = this.firstOrNull { it.isBasePlan } ?: return null

    val validOffers = this
        .filter { !it.isBasePlan }
        .filter { !it.tags.contains("rc-ignore-default-offer") }

    return findLongestFreeTrial(validOffers) ?: findLowestNonFreeOffer(validOffers) ?: basePlan
}

private fun findLongestFreeTrial(offers: List<GoogleSubscriptionOption>): GoogleSubscriptionOption? {
    return offers.mapNotNull { offer ->
        offer.freePricingPhase?.let { pricingPhase ->
            Pair(offer, billingPeriodToDays(pricingPhase.billingPeriod))
        }
    }.maxByOrNull { it.second }?.first
}

private fun findLowestNonFreeOffer(offers: List<GoogleSubscriptionOption>): GoogleSubscriptionOption? {
    return offers.mapNotNull { offer ->
        offer.nonFreePricingPhase?.let { pricingPhase ->
            Pair(offer, pricingPhase.price.amountMicros)
        }
    }.minByOrNull { it.second }?.first
}

private val GoogleSubscriptionOption.freePricingPhase: PricingPhase?
    get() = pricingPhases.firstOrNull()?.takeIf { it.price.amountMicros == 0L }

private val GoogleSubscriptionOption.nonFreePricingPhase: PricingPhase?
    get() = pricingPhases.firstOrNull()?.takeIf { it.price.amountMicros > 0L }

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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun billingPeriodToDays(period: Period): Int {
    val days = DAYS_IN_UNIT[period.unit] ?: 0
    return period.value * days
}
