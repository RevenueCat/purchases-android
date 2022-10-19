package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

/*fun SkuDetails.toStoreProduct() =
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
    )*/

fun ProductDetails.PricingPhases.readable(): String {
    return "[" + pricingPhaseList.map { (if (it.billingCycleCount > 1) (it.billingCycleCount.toString() + "*") else "") + it.billingPeriod + "@" + it.formattedPrice }
        .joinToString() + "]"
}

fun ProductDetails.toStoreProduct(offerToken: String, pricingPhases: ProductDetails.PricingPhases, offerTags: List<String>) =
    StoreProduct(
        productId,
        ProductType.SUBS,
        pricingPhases.readable(),
        100,
        "USD",
        "originalPrice",
        100,
        title,
        offerTags.toString(),
        pricingPhases.pricingPhaseList.last().billingPeriod,
        null,
        null,
        0,
        null,
        0,
        "icon",
        JSONObject(mapOf("offerToken" to offerToken)),
        pricingPhases.pricingPhaseList.map { it -> PricingPhase(
            it.billingPeriod,
            it.billingCycleCount,
            it.formattedPrice,
            it.priceAmountMicros,
            it.priceCurrencyCode,
            it.recurrenceMode) },
        this,
        offerToken
    )
