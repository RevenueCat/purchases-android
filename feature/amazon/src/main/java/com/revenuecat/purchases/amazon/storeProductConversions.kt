package com.revenuecat.purchases.amazon

import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.model.Product
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject
import java.util.regex.Pattern
import com.amazon.device.iap.model.ProductType as AmazonProductType

val StoreProduct.amazonProduct: Product
    get() = ProductBuilder()
        .setSku(originalJson.getString("sku"))
        .setProductType(originalJson.getProductType("productType"))
        .setDescription(originalJson.getString("description"))
        .setPrice(originalJson.getString("price"))
        .setSmallIconUrl(originalJson.getString("smallIconUrl"))
        .setTitle(originalJson.getString("title"))
        .setCoinsRewardAmount(originalJson.getInt("coinsRewardAmount")).build()

fun Product.toStoreProduct(marketplace: String): StoreProduct {
    // By default, Amazon automatically converts the base list price of your IAP items into
    // the local currency of each marketplace where they can be sold, and customers will see IAP items in English.
    val (currencyCode, priceAmountMicros) = price.extractPrice(marketplace)

    return StoreProduct(
        sku,
        productType.toRevenueCatProductType(),
        price,
        priceAmountMicros = priceAmountMicros,
        priceCurrencyCode = currencyCode,
        originalPrice = null,
        originalPriceAmountMicros = 0,
        title,
        description,
        subscriptionPeriod = null,
        freeTrialPeriod = null,
        introductoryPrice = null,
        introductoryPriceAmountMicros = 0,
        introductoryPricePeriod = null,
        introductoryPriceCycles = 0,
        iconUrl = smallIconUrl,
        originalJson = toJSON()
    )
}

internal fun String.extractPrice(marketplace: String): Price {
    val priceNumeric = this.parsePriceUsingRegex() ?: 0.0f

    val currencyCode = ISO3166Alpha2ToISO42170Converter.convertOrEmpty(marketplace)

    return Price(
        currencyCode,
        priceAmountMicros = priceNumeric.times(MICROS_MULTIPLIER).toLong()
    )
}

internal data class Price(
    val currencyCode: String,
    val priceAmountMicros: Long
)

// Explanations about the regexp:
// \\d+: match the first(s) number(s)
// [\\.,\\s]: match a "separator": a dot, comma or space
// \\d+ (the second one): match the number(s) after the separator.
// The lasts two are englobed in []*, as they can be repeated 0 or n times.
private val pattern: Pattern = Pattern.compile("(\\d+[[\\.,\\s]\\d+]*)")

internal fun String.parsePriceUsingRegex(): Float? {
    val matcher = pattern.matcher(this)
    return if (matcher.find()) {
        val dirtyPrice = matcher.group()
        var price: String
        // 2 355 825.837
        price = dirtyPrice.replace(" ", "")
        val hasCommas = price.contains(",")
        if (hasCommas) {
            val numberOfCommas = price.length - price.replace(",", "").length
            price = if (price.contains(".") || numberOfCommas > 1) {
                // 1,000,000.00
                price.replace(",", "")
            } else {
                // 1,00
                price.replace(",", ".")
            }
        }
        price.toFloat()
    } else null
}

private fun JSONObject.getProductType(productType: String) =
    AmazonProductType.values().firstOrNull { it.name == this.getString(productType) }
