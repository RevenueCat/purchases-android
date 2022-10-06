package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BC5StoreProduct
import com.revenuecat.purchases.models.StoreProductImpl
import org.json.JSONObject

fun SkuDetails.toStoreProduct() =
    StoreProductImpl(
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
        JSONObject(originalJson)
    )

fun ProductDetails.toStoreProduct(offerToken: String, pricingPhases: ProductDetails.PricingPhases) =
    BC5StoreProduct(
        productId,
        ProductType.SUBS,
        "price",
        100,
        "USD",
        "originalPrice",
        100,
        title,
        description,
        pricingPhases.pricingPhaseList.last().billingPeriod,
        null,
        null,
        0,
        null,
        0,
        "icon",
        JSONObject("{}"),
        this,
        offerToken,
        pricingPhases
    )
