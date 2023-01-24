@file:Suppress("MaximumLineLength", "MaxLineLength")
package com.revenuecat.purchases.google

import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode

fun List<GoogleSubscriptionOption>.findBestOffer(): GoogleSubscriptionOption? {
    val basePlan = this.firstOrNull { it.isBasePlan } ?: return null

    val validOffers = this
        .filter { !it.isBasePlan }
        .filter { !it.tags.contains("rc-ignore-best-offer") }

    return findLongestFreeTrial(validOffers) ?: findBestSavingsOffer(basePlan, validOffers) ?: basePlan
}

private fun findLongestFreeTrial(offers: List<GoogleSubscriptionOption>): GoogleSubscriptionOption? {
    return offers.mapNotNull { offer ->
        // Finds longest free pricing phase for an offer
        offer.pricingPhases.filter { pricingPhase ->
            pricingPhase.priceAmountMicros == 0L
        }.map {
            Pair(offer, parseBillPeriodToDays(it.billingPeriod)) // putting offer in pair here works but feels weird
        }.maxByOrNull { it.second }
    }.maxByOrNull { it.second }?.first
}

@Suppress("ReturnCount")
private fun findBestSavingsOffer(
    basePlan: GoogleSubscriptionOption,
    offers: List<GoogleSubscriptionOption>
): GoogleSubscriptionOption? {
    val basePlanPricingPhase = basePlan.pricingPhases.firstOrNull() ?: return null

    val longestOfferTotalDays = offers
        .mapNotNull { subscriptionOption ->
            subscriptionOption.pricingPhases.sumOf { it.totalNumberOfDays() ?: 0 }
        }.maxByOrNull { it } ?: 0

    if (longestOfferTotalDays < 0) {
        return null
    }

    val periodDays = parseBillPeriodToDays(basePlanPricingPhase.billingPeriod)
    val basePlanCostPerDay = basePlanPricingPhase.priceAmountMicros / periodDays

    val offersWithCheapestPriceByDay = offers.map { offer ->

        // Get normalized pricing for each phase
        val pricePerDayPerPhase = offer.pricingPhases
            .filter { it.recurrenceMode == RecurrenceMode.FINITE_RECURRING }
            .mapNotNull { pricingPhase ->
                val periodDays = parseBillPeriodToDays(pricingPhase.billingPeriod)

                if (periodDays > 0) {
                    val pricePerDay = pricingPhase.priceAmountMicros / periodDays
                    val totalDaysOfPhase = pricingPhase.totalNumberOfDays() ?: 0

                    val numberOfDaysOnBasePlan = longestOfferTotalDays - totalDaysOfPhase

                    val costOfPricingPhase = totalDaysOfPhase * pricePerDay
                    val costOfBasePlan = numberOfDaysOnBasePlan * basePlanCostPerDay

                    val totes = costOfPricingPhase + costOfBasePlan

                    Pair(pricingPhase, totes)
                } else {
                    null
                }
            }

        // Totals normalized pricing
        var totalPricePerPhase = 0L
        pricePerDayPerPhase.forEach { totalPricePerPhase += it.second }

        Pair(offer, totalPricePerPhase)
    }.sortedBy { it.second }

    return offersWithCheapestPriceByDay.firstOrNull()?.first
}

private fun PricingPhase.totalNumberOfDays(): Int? {
    val periodDays = parseBillPeriodToDays(billingPeriod)
    val cycleCount = billingCycleCount ?: 0

    return if (periodDays > 0 && cycleCount > 0) {
        periodDays * cycleCount
    } else {
        0
    }
}

private const val DAYS_IN_YEAR = 365
private const val DAYS_IN_MONTH = 30
private const val DAYS_IN_WEEK = 7

// Would use Duration.parse but only available API 26 and up
internal fun parseBillPeriodToDays(period: String): Int {
    // Takes from https://stackoverflow.com/a/32045167
    val regex = "^P(?!\$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?(T(?=\\d)(\\d+(?:\\.\\d+)?H)?(\\d+(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?S)?)?\$"
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
