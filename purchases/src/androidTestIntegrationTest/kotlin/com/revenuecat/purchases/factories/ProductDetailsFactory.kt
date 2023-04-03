package com.revenuecat.purchases.factories

import com.revenuecat.purchases.Constants
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.ProductDetails
import org.json.JSONObject

object ProductDetailsFactory {

    private const val validStoreProductJson = "{" +
        "\"productId\":\"${Constants.productIdToPurchase}\"," +
        "\"type\":\"subs\"," +
        "\"title\":\"Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)\"," +
        "\"name\":\"Monthly Product Intro Pricing One Week\"," +
        "\"description\":\"Monthly Product Intro Pricing One Week\"," +
        "\"price\":\"€5.49\"," +
        "\"price_amount_micros\":5490000," +
        "\"price_currency_code\":\"EUR\"," +
        "\"skuDetailsToken\":\"test-token\"," +
        "\"subscriptionPeriod\":\"P1M\"," +
        "\"freeTrialPeriod\":\"P1W\"" +
        "}"

    @Suppress("LongParameterList")
    fun createProductDetails(
        sku: String = Constants.productIdToPurchase,
        type: ProductType = ProductType.SUBS,
        price: String = "€5.49",
        priceAmountMicros: Long = 5490000,
        priceCurrencyCode: String = "EUR",
        originalPrice: String? = "€5.49",
        originalPriceAmountMicros: Long = 5490000,
        title: String = "Monthly Product Intro Pricing One Week (RevenueCat SDK Tester)",
        description: String = "Monthly Product Intro Pricing One Week",
        subscriptionPeriod: String? = "P1M",
        freeTrialPeriod: String? = "P1W",
        introductoryPrice: String? = null,
        introductoryPriceAmountMicros: Long = 0,
        introductoryPricePeriod: String? = null,
        introductoryPriceCycles: Int = 0,
        iconUrl: String = "",
        originalJson: JSONObject = JSONObject(validStoreProductJson)
    ): ProductDetails {
        return ProductDetails(
            sku = sku,
            type = type,
            price = price,
            priceAmountMicros = priceAmountMicros,
            priceCurrencyCode = priceCurrencyCode,
            originalPrice = originalPrice,
            originalPriceAmountMicros = originalPriceAmountMicros,
            title = title,
            description = description,
            subscriptionPeriod = subscriptionPeriod,
            freeTrialPeriod = freeTrialPeriod,
            introductoryPrice = introductoryPrice,
            introductoryPriceAmountMicros = introductoryPriceAmountMicros,
            introductoryPricePeriod = introductoryPricePeriod,
            introductoryPriceCycles = introductoryPriceCycles,
            iconUrl = iconUrl,
            originalJson = originalJson
        )
    }
}
