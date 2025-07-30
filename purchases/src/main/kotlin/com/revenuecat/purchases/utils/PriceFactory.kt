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
        val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            // Making sure we do not add spurious digits:
            val digits = currency.defaultFractionDigits.coerceAtLeast(0)
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }

        val formatted = numberFormat.format(amountMicros / MICRO_MULTIPLIER)

        return Price(formatted, amountMicros, currencyCode)
    }
}
