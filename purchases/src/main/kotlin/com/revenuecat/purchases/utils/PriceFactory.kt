@file:JvmSynthetic

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.Price
import java.math.BigDecimal
import java.math.RoundingMode
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
        truncatePrice: Boolean = false,
    ): Price {
        val currency = Currency.getInstance(currencyCode)
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)

        val valueInCurrency = amountMicros / MICRO_MULTIPLIER
        val adjustedValue = if (truncatePrice) {
            valueInCurrency.roundToDecimalPlaces(digits)
        } else {
            BigDecimal.valueOf(valueInCurrency)
                .setScale(digits, RoundingMode.HALF_UP)
                .toDouble()
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
