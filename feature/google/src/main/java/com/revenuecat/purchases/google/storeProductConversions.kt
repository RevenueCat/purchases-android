package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

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
        null
    )

fun ProductDetails.toStoreProduct() =
    StoreProduct(
        productId,
        ProductType.SUBS,
        "price",
        100,
        "USD",
        "originalPrice",
        99,
        title,
        description,
        "P1Y",
        "P1M",
        "$1",
        98,
        "P1M",
        123,
        "icon",
        JSONObject("{}"),
        this
    )
