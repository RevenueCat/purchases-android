package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import java.util.Locale

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class PricingPhaseAPI {
    fun checkPricingPhase(pricingPhase: PricingPhase, locale: Locale) {
        val billingPeriod: Period = pricingPhase.billingPeriod
        val recurrenceMode: RecurrenceMode = pricingPhase.recurrenceMode
        val billingCycleCount: Int? = pricingPhase.billingCycleCount
        val price: Price = pricingPhase.price
        val pricePerMonth: String = pricingPhase.formattedPriceInMonths(locale)
        val pricePerMonthNoLocale: String = pricingPhase.formattedPriceInMonths()

        val offerPaymentMode: OfferPaymentMode? = pricingPhase.offerPaymentMode
    }

    fun checkingPrice(price: Price) {
        val formatted: String = price.formatted
        val amountMicros: Long = price.amountMicros
        val currencyCode: String = price.currencyCode
    }
}
