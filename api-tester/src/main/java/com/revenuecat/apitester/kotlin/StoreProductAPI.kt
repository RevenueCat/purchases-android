package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.RCProductType
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        with(product) {
            val sku: String = sku
            val type: RCProductType = type
            val price: String = price
            val priceAmountMicros: Long = priceAmountMicros
            val priceCurrencyCode: String = priceCurrencyCode
            val originalPrice: String? = originalPrice
            val originalPriceAmountMicros: Long = originalPriceAmountMicros
            val title: String = title
            val description: String = description
            val subscriptionPeriod: String? = subscriptionPeriod
            val freeTrialPeriod: String? = freeTrialPeriod
            val introductoryPrice: String? = introductoryPrice
            val introductoryPriceAmountMicros: Long = introductoryPriceAmountMicros
            val introductoryPricePeriod: String? = introductoryPricePeriod
            val introductoryPriceCycles: Int = introductoryPriceCycles
            val iconUrl: String = iconUrl
            val originalJson: JSONObject = originalJson
        }
    }

    fun check(type: RCProductType) {
        when (type) {
            RCProductType.SUBS,
            RCProductType.INAPP,
            RCProductType.UNKNOWN
            -> {}
        }.exhaustive
    }
}
