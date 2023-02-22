package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.toRecurrenceMode
import com.revenuecat.purchases.models.toPeriod

fun ProductDetails.PricingPhase.toRevenueCatPricingPhase(): PricingPhase {
    return PricingPhase(
        billingPeriod.toPeriod(),
        recurrenceMode.toRecurrenceMode(),
        billingCycleCount,
        Price(formattedPrice, priceAmountMicros, priceCurrencyCode)
    )
}
