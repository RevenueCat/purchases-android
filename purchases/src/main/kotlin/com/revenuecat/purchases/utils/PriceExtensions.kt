package com.revenuecat.purchases.utils

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val MICRO_MULTIPLIER = 1_000_000.0

internal fun Price.formattedPricePerMonth(billingPeriod: Period, locale: Locale): String {
    val periodMonths = billingPeriod.valueInMonths
    val currencyCode = currencyCode
    val priceDecimal = amountMicros / MICRO_MULTIPLIER
    val numberFormat = NumberFormat.getCurrencyInstance(locale)
    numberFormat.currency = Currency.getInstance(currencyCode)
    return numberFormat.format(priceDecimal / periodMonths)
}
