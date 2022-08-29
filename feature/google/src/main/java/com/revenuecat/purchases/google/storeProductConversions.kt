package com.revenuecat.purchases.google

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject
import toProductType

fun SkuDetails.toStoreProduct() =
    StoreProduct(
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
