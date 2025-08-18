@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.PriceFactory
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

private const val MICRO_MULTIPLIER = 1_000_000.0

@JvmSynthetic
internal fun Price.localized(locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
    // always round if rounding on
    return if (showZeroDecimalPlacePrices && this.endsIn00Cents()) {
        this.getTruncatedFormatted(locale)
    } else {
        PriceFactory.createPrice(amountMicros, currencyCode, locale).formatted
    }
}

@JvmSynthetic
internal fun Price.localizedPerPeriod(period: Period, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
    val localizedPrice = this.localized(locale, showZeroDecimalPlacePrices)
    val formattedPeriod = period.localizedAbbreviatedPeriod(locale)
    return "$localizedPrice/$formattedPeriod"
}

/**
 * Returns price, rounded with the cents component truncated, formatted for the given locale.
 * For example $3.
 */
private fun Price.getTruncatedFormatted(locale: Locale = Locale.getDefault()): String {
    val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
        maximumFractionDigits = 0
    }
    val amount = amountMicros / MICRO_MULTIPLIER
    return numberFormat.format(amount)
}

@SuppressWarnings("MagicNumber")
private fun Price.endsIn00Cents(): Boolean {
    val normalPrice = amountMicros / MICRO_MULTIPLIER
    val roundedPrice = (normalPrice * 100).roundToLong() / 100.0
    return roundedPrice * 100 % 100 == 0.0
}
