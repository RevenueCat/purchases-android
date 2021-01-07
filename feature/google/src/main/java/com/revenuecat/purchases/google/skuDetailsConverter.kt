package com.revenuecat.purchases.google

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.models.ProductDetails
import org.json.JSONObject

val ProductDetails.skuDetails: SkuDetails
    get() = SkuDetails(this.originalJson.toString())

fun SkuDetails.toProductDetails() =
    ProductDetails(
        sku,
        type.toProductType(),
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
        JSONObject(originalJson)
    )
