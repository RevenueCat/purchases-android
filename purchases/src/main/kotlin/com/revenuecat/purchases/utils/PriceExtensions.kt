package com.revenuecat.purchases.utils

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val MICRO_MULTIPLIER = 1_000_000.0

internal fun Price.pricePerMonth(billingPeriod: Period, locale: Locale): Price {
    val periodMonths = billingPeriod.valueInMonths
    val numberFormat = NumberFormat.getCurrencyInstance(locale)
    numberFormat.currency = Currency.getInstance(currencyCode)

    val value = amountMicros / periodMonths
    val formatted = numberFormat.format(value / MICRO_MULTIPLIER)

    return Price(formatted, (value).toLong(), currencyCode)
}
