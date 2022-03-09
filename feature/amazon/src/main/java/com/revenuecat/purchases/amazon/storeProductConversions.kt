package com.revenuecat.purchases.amazon

import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.model.Product
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject
import java.math.BigDecimal
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
    val priceNumeric = this.parsePriceUsingRegex() ?: BigDecimal.ZERO
    val priceAmountMicros = (priceNumeric * BigDecimal(MICROS_MULTIPLIER)).toLong()
    val currencyCode = ISO3166Alpha2ToISO42170Converter.convertOrEmpty(marketplace)

    return Price(
        currencyCode,
        priceAmountMicros
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

internal fun String.parsePriceUsingRegex(): BigDecimal? {
    val matcher = pattern.matcher(this)
    return matcher.takeIf { it.find() }?.let {
        val dirtyPrice = matcher.group()
        var price = dirtyPrice.replace(" ", "")
        val split = price.split(".", ",")
        if (split.size != 1) {
            // Assuming all prices we get have 2 decimal points
            // Most currencies but Dirhan use 2 decimals. Amazon doesn't support Dirhan at the moment
            price = if (split.last().length == 3) {
                price.replace(".", "").replace(",", "")
            } else {
                val intPart = split.dropLast(1).joinToString("")
                "$intPart.${split.last()}"
            }
        }
        price = price.trim()
        BigDecimal(price)
    }
}

private fun JSONObject.getProductType(productType: String) =
    AmazonProductType.values().firstOrNull { it.name == this.getString(productType) }
