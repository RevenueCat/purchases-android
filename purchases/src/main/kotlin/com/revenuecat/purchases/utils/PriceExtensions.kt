package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import java.util.Locale

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
    val value = amountMicros / units
    return PriceFactory.createPrice(value.toLong(), currencyCode, locale)
}
