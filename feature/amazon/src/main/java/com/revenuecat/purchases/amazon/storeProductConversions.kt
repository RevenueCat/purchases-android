package com.revenuecat.purchases.amazon

import android.os.Build
import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.model.Product
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale
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
    val (currencyCode, priceAmountMicros) =
        price.extractPrice(
            currency = Currency.getInstance(Locale("EN", marketplace)),
            numberFormat = NumberFormat.getInstance()
        )

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

internal fun String.extractPrice(
    currency: Currency,
    numberFormat: NumberFormat
): Price {
    val (priceNumeric, currencySymbol) =
        this.parsePriceAndCurrencySymbolUsingRegex(numberFormat) ?: 0.0f to currency.symbol

    var foundCurrencyCode: String? = null
    if (currencySymbol == currency.symbol) {
        foundCurrencyCode = currency.currencyCode
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // We've seen cases the price being as "US$", but the marketplace currency code being "INR"
            debugLog(AmazonStrings.PRICE_EXTRACTION_ITERATING_OVER_AVAILABLE_CURRENCIES)
            foundCurrencyCode =
                Currency.getAvailableCurrencies()
                    .firstOrNull { it.symbol == currencySymbol }?.currencyCode
        }
    }

    val currencyCode = foundCurrencyCode ?: run {
        debugLog(AmazonStrings.PRICE_EXTRACTION_USING_CURRENCY_SYMBOL)
        currencySymbol
    }

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

internal fun String.parsePriceAndCurrencySymbolUsingRegex(numberFormat: NumberFormat): Pair<Float, String>? {
    val matcher = pattern.matcher(this)
    return if (matcher.find()) {
        val price: String = matcher.group()
        val currencySymbol = this.replace(price, "").trim()
        val priceNumeric = extractPriceNumber(price, numberFormat)

        priceNumeric to currencySymbol
    } else null
}

private fun extractPriceNumber(
    price: String,
    numberFormat: NumberFormat
): Float {
    return price.trim().let { formattedPriceWithoutSymbol ->
        try {
            numberFormat.parse(formattedPriceWithoutSymbol).toFloat()
        } catch (e: ParseException) {
            0.0f
        }
    }
}

private fun JSONObject.getProductType(productType: String) =
    AmazonProductType.values().firstOrNull { it.name == this.getString(productType) }
