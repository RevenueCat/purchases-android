package com.revenuecat.purchases.google

import com.revenuecat.purchases.models.GoogleSubscriptionOption
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
            Pair(offer, parseBillPeriodToDays(pricingPhase.billingPeriod))
        }
    }.maxByOrNull { it.second }?.first
}

private fun findLowestNonFreeOffer(offers: List<GoogleSubscriptionOption>): GoogleSubscriptionOption? {
    return offers.mapNotNull { offer ->
        offer.nonFreePricingPhase?.let { pricingPhase ->
            Pair(offer, pricingPhase.price.priceAmountMicros)
        }
    }.minByOrNull { it.second }?.first
}

private val GoogleSubscriptionOption.freePricingPhase: PricingPhase?
    get() = pricingPhases.firstOrNull()?.takeIf { it.price.priceAmountMicros == 0L }

private val GoogleSubscriptionOption.nonFreePricingPhase: PricingPhase?
    get() = pricingPhases.firstOrNull()?.takeIf { it.price.priceAmountMicros > 0L }

private const val DAYS_IN_YEAR = 365
private const val DAYS_IN_MONTH = 30
private const val DAYS_IN_WEEK = 7

// Would use Duration.parse but only available API 26 and up
internal fun parseBillPeriodToDays(period: String): Int {
    // Takes from https://stackoverflow.com/a/32045167
    val regex = "^P(?!\$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?\$"
        .toRegex()
        .matchEntire(period)

    regex?.let { periodResult ->
        val toInt = fun(part: String): Int {
            return part.dropLast(1).toIntOrNull() ?: 0
        }

        val (year, month, week, day) = periodResult.destructured
        return (toInt(year) * DAYS_IN_YEAR) + (toInt(month) * DAYS_IN_MONTH) + (toInt(week) * DAYS_IN_WEEK) + toInt(day)
    }

    return 0
}
