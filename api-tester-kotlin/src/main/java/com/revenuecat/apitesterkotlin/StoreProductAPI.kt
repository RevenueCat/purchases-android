package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

@Suppress("unused")
private class StoreProductAPI {
    fun check(product: StoreProduct) {
        val sku: String = product.sku
        val type: ProductType = product.type
        val price: String = product.price
        val priceAmountMicros: Long = product.priceAmountMicros
        val priceCurrencyCode: String = product.priceCurrencyCode
        val originalPrice: String? = product.originalPrice
        val originalPriceAmountMicros: Long = product.originalPriceAmountMicros
        val title: String = product.title
        val description: String = product.description
        val subscriptionPeriod: String? = product.subscriptionPeriod
        val freeTrialPeriod: String? = product.freeTrialPeriod
        val introductoryPrice: String? = product.introductoryPrice
        val introductoryPriceAmountMicros: Long = product.introductoryPriceAmountMicros
        val introductoryPricePeriod: String? = product.introductoryPricePeriod
        val introductoryPriceCycles: Int = product.introductoryPriceCycles
        val iconUrl: String = product.iconUrl
        val originalJson: JSONObject = product.originalJson
    }

    fun check(type: ProductType) {
        when (type) {
            ProductType.SUBS,
            ProductType.INAPP,
            ProductType.UNKNOWN
            -> {}
        }
    }
}
