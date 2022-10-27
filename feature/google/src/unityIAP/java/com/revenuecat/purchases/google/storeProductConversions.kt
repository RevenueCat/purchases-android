package com.revenuecat.purchases.google

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

private fun SkuDetails.toStoreProduct() =
    BC4StoreProduct(
        sku,
        type.toRevenueCatProductType(),
        if (type == ProductType.INAPP) Price(price, priceAmountMicros, priceCurrencyCode) else null,
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        freeTrialPeriod.takeIf { it.isNotBlank() },
        iconUrl,
        JSONObject(originalJson),
        this
    )