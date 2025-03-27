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

private fun Price.pricePerPeriod(units: Double, locale: Locale): Price {
    val currency = Currency.getInstance(currencyCode)
    val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        // Making sure we do not add spurious digits:
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        maximumFractionDigits = digits
        minimumFractionDigits = digits
    }

    val value = amountMicros / units
    val formatted = numberFormat.format(value / MICRO_MULTIPLIER)

    return Price(formatted, (value).toLong(), currencyCode)
}
