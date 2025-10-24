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
        val pricePerMonthString: String = pricingPhase.formattedPriceInMonths(locale)
        val pricePerMonthStringNoLocale: String = pricingPhase.formattedPriceInMonths()
        val pricePerDay = pricingPhase.pricePerDay(locale)
        val pricePerWeek = pricingPhase.pricePerWeek(locale)
        val pricePerMonth = pricingPhase.pricePerMonth(locale)
        val pricePerYear = pricingPhase.pricePerYear(locale)
        val pricePerDayNoLocale = pricingPhase.pricePerDay()
        val pricePerWeekNoLocale = pricingPhase.pricePerWeek()
        val pricePerMonthNoLocale = pricingPhase.pricePerMonth()
        val pricePerYearNoLocale = pricingPhase.pricePerYear()

        val offerPaymentMode: OfferPaymentMode? = pricingPhase.offerPaymentMode
    }

    fun checkingPrice(price: Price) {
        val formatted: String = price.formatted
        val amountMicros: Long = price.amountMicros
        val currencyCode: String = price.currencyCode
    }
}
