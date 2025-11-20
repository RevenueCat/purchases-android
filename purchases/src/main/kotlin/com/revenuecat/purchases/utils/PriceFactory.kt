@file:JvmSynthetic

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.Price
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal object PriceFactory {

    @JvmSynthetic
    @OptIn(InternalRevenueCatAPI::class)
    internal fun createPrice(
        amountMicros: Long,
        currencyCode: String,
        locale: Locale,
    ): Price {
        val currency = Currency.getInstance(currencyCode)
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)

        val valueInCurrency = amountMicros / MICRO_MULTIPLIER
        val truncatedValue = valueInCurrency.roundToDecimalPlaces(digits)

        val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }

        val formatted = numberFormat.format(truncatedValue)

        return Price(formatted, amountMicros, currencyCode)
    }
}
