package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.toRecurrenceMode

fun ProductDetails.PricingPhase.toRevenueCatPricingPhase(): PricingPhase {
    return PricingPhase(
        billingPeriod,
        recurrenceMode.toRecurrenceMode(),
        billingCycleCount,
        Price(formattedPrice, priceAmountMicros, priceCurrencyCode)
    )
}
