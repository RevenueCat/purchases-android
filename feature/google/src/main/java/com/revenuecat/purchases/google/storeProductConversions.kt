package com.revenuecat.purchases.google

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject
import java.text.NumberFormat
/*
fun SkuDetails.toStoreProduct() =
    StoreProduct(
        sku,
        type.toRevenueCatProductType(),
        price,
        priceAmountMicros,
        priceCurrencyCode,
        originalPrice,
        originalPriceAmountMicros,
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        freeTrialPeriod.takeIf { it.isNotBlank() },
        introductoryPrice.takeIf { it.isNotBlank() },
        introductoryPriceAmountMicros,
        introductoryPricePeriod.takeIf { it.isNotBlank() },
        introductoryPriceCycles,
        iconUrl,
        JSONObject(originalJson),
        getPricingPhases()
    )

fun SkuDetails.getPricingPhases(): List<PricingPhase> {
    if (subscriptionPeriod.isBlank()) {
        return emptyList() // TODO what should we do for one time purchases?
    }
    val phases = mutableListOf<PricingPhase>()
    if (freeTrialPeriod.isNotBlank()) {
        phases.add(
            PricingPhase(
                freeTrialPeriod,
                1,
                PricingPhase.FORMATTED_PRICE_FREE,
                0,
                priceCurrencyCode,
                PricingPhase.FINITE_RECURRING
            )
        )
    }
    if (introductoryPricePeriod.isNotBlank()) {
        phases.add(
            PricingPhase(
                introductoryPricePeriod,
                introductoryPriceCycles,
                "", //TODO format price
                introductoryPriceAmountMicros,
                priceCurrencyCode,
                PricingPhase.FINITE_RECURRING
            )
        )
    }
    phases.add(
        PricingPhase(
            subscriptionPeriod,
            0,
            price,
            priceAmountMicros,
            priceCurrencyCode,
            PricingPhase.INFINITE_RECURRING
        )
    )
    return phases
}
*/