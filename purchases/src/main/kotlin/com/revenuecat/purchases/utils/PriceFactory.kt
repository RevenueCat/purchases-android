@file:JvmSynthetic

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.Price
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal object PriceFactory {

    /**
     * Creates a Price from the given amount in micros.
     *
     * @param amountMicros The price amount in micros (1/1,000,000 of the currency unit).
     * @param currencyCode The ISO 4217 currency code.
     * @param locale The locale to use for formatting.
     * @param truncatePrice If true, uses floor to truncate the price (for derived prices like
     *        pricePerMonth where we don't want to round up). If false (default), uses half-up
     *        rounding which correctly handles floating-point imprecision for exact catalog prices.
     */
    @JvmSynthetic
    @OptIn(InternalRevenueCatAPI::class)
    internal fun createPrice(
        amountMicros: Long,
        currencyCode: String,
        locale: Locale,
        truncatePrice: Boolean = false,
    ): Price {
        val currency = Currency.getInstance(currencyCode)
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)

        val valueInCurrency = amountMicros / MICRO_MULTIPLIER
        val adjustedValue = if (truncatePrice) {
            valueInCurrency.truncateToDecimalPlaces(digits)
        } else {
            valueInCurrency.roundToDecimalPlaces(digits)
        }

        val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }

        val formatted = numberFormat.format(adjustedValue)

        return Price(formatted, amountMicros, currencyCode)
    }
}
