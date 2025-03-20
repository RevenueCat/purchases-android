package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val MICRO_MULTIPLIER = 1_000_000.0

@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun Price.pricePerDay(billingPeriod: Period, locale: Locale): Price {
    return pricePerPeriod(billingPeriod.valueInDays, locale)
}

@OptIn(InternalRevenueCatAPI::class)
internal fun Price.pricePerWeek(billingPeriod: Period, locale: Locale): Price {
    return pricePerPeriod(billingPeriod.valueInWeeks, locale)
}

internal fun Price.pricePerMonth(billingPeriod: Period, locale: Locale): Price {
    return pricePerPeriod(billingPeriod.valueInMonths, locale)
}

@OptIn(InternalRevenueCatAPI::class)
internal fun Price.pricePerYear(billingPeriod: Period, locale: Locale): Price {
    return pricePerPeriod(billingPeriod.valueInYears, locale)
}

internal val Price.numDecimals: Int
    get() {
        println("TESTING micros: $amountMicros")
        println("TESTING formatted: $formatted")

        val fractionMicros = amountMicros % 1_000_000
        println("TESTING fractionMicros: $fractionMicros")

        val regex = Regex("([.,])(\\d+)$")
        val match = regex.find(formatted) ?: return 0

        val candidateSep = match.groupValues[1] // The separator character from the match.
        val candidateFraction = match.groupValues[2] // The digits following the separator.
        val candidateFractionLength = candidateFraction.length

        return candidateFractionLength
    }

private fun Price.pricePerPeriod(units: Double, locale: Locale): Price {
    val numberFormat = NumberFormat.getCurrencyInstance(locale)
    numberFormat.currency = Currency.getInstance(currencyCode)

    val value = amountMicros / units
    val formatted = numberFormat.format(value / MICRO_MULTIPLIER)

    return Price(formatted, (value).toLong(), currencyCode)
}
