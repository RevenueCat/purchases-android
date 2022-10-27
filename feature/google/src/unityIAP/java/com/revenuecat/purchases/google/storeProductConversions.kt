package com.revenuecat.purchases.google

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

private fun SkuDetails.toStoreProduct() =
    BC4StoreProduct(
        sku,
        type.toRevenueCatProductType(),
        createPrice(),
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        createPurchaseOptions(),
        freeTrialPeriod,
        introductoryPrice,
        introductoryPriceAmountMicros,
        introductoryPricePeriod,
        introductoryPriceCycles,
        iconUrl,
        originalPrice,
        originalPriceAmountMicros,
        JSONObject(originalJson)
    )

private fun SkuDetails.createPrice(): Price? {
    return if (type.toRevenueCatProductType() == ProductType.INAPP)
        Price(price, priceAmountMicros, priceCurrencyCode)
    else null
}

private fun SkuDetails.createPurchaseOptions(): List<PurchaseOption> {
    return if (type.toRevenueCatProductType() == ProductType.SUBS) {
        val subPricingPhase =
            PricingPhase(
                subscriptionPeriod,
                priceCurrencyCode,
                price,
                priceAmountMicros,
                RecurrenceMode.INFINITE_RECURRING, //TODO confirm if this is the value we want
                0
            )
        return listOf(PurchaseOption(listOf(subPricingPhase)))
    } else listOf()

}