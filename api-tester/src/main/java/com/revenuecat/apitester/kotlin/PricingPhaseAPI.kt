package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class PricingPhaseAPI {
    fun checkPricingPhase(pricingPhase: PricingPhase) {
        val billingPeriod: Period = pricingPhase.billingPeriod
        val recurrenceMode: RecurrenceMode = pricingPhase.recurrenceMode
        val billingCycleCount: Int? = pricingPhase.billingCycleCount
        val price: Price = pricingPhase.price

        val offerPaymentMode: OfferPaymentMode? = pricingPhase.offerPaymentMode
    }
}
