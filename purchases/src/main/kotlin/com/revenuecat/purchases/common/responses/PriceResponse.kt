package com.revenuecat.purchases.common.responses

import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.MICRO_MULTIPLIER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Serializable
internal class PriceResponse(
    @SerialName("amount")
    val amount: Double,
    @SerialName("currency")
    val currencyCode: String,
) {

    override fun toString(): String {
        return """
            PriceResponse {
                amount: $amount,
                currencyCode: $currencyCode
            }
        """.trimIndent()
    }

    fun toPrice(locale: Locale): Price {
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        numberFormat.currency = Currency.getInstance(currencyCode)

        val formatted = numberFormat.format(amount)

        return Price(formatted, (amount * MICRO_MULTIPLIER).toLong(), currencyCode)
    }
}
