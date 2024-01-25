package com.revenuecat.purchases.amazon

import com.amazon.device.iap.model.Product
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import java.math.BigDecimal
import java.util.regex.Pattern

internal fun Product.toStoreProduct(marketplace: String): StoreProduct? {
    if (price == null) {
        log(LogIntent.AMAZON_ERROR, AmazonStrings.PRODUCT_PRICE_MISSING.format(sku))
        return null
    }
    // By default, Amazon automatically converts the base list price of your IAP items into
    // the local currency of each marketplace where they can be sold, and customers will see IAP items in English.
    val priceInfo = price.createPrice(marketplace)

    return AmazonStoreProduct(
        sku,
        productType.toRevenueCatProductType(),
        title,
        title,
        description,
        period = subscriptionPeriod?.createPeriod(),
        priceInfo,
        null,
        defaultOption = null,
        iconUrl = smallIconUrl,
        freeTrialPeriod = freeTrialPeriod?.createPeriod(),
        originalProductJSON = this.toJSON(),
        presentedOfferingIdentifier = null,
    )
}

@SuppressWarnings("MagicNumber")
internal fun String.createPeriod(): Period? {
    // Valid values: Weekly, BiWeekly, Monthly, BiMonthly, Quarterly, SemiAnnually, Annually.
    // https://developer.amazon.com/docs/in-app-purchasing/iap-implement-iap.html#successful-reques

    return when (this) {
        "Weekly" -> Period(1, Period.Unit.WEEK, "P1W")
        "BiWeekly" -> Period(2, Period.Unit.WEEK, "P2W")
        "Monthly" -> Period(1, Period.Unit.MONTH, "P1M")
        "BiMonthly" -> Period(2, Period.Unit.MONTH, "P2M")
        "Quarterly" -> Period(3, Period.Unit.MONTH, "P3M")
        "SemiAnnually" -> Period(6, Period.Unit.MONTH, "P6M")
        "Annually" -> Period(1, Period.Unit.YEAR, "P1Y")

        // Handle "7 Days" or "14 Days" or "1 Month" just in case
        else -> this.split(" ")
            .takeIf { it.size == 2 }
            ?.let {
                it.firstOrNull()?.toIntOrNull()?.let { numberValue ->
                    val letter = it[1].first().uppercase()
                    val iso = "P$numberValue$letter"
                    return Period.create(iso)
                }
            }
    }
}

internal fun String.createPrice(marketplace: String): Price {
    val priceNumeric = this.parsePriceUsingRegex() ?: BigDecimal.ZERO
    val priceAmountMicros = (priceNumeric * BigDecimal(MICROS_MULTIPLIER)).toLong()
    val currencyCode = ISO3166Alpha2ToISO42170Converter.convertOrEmpty(marketplace)

    return Price(this, priceAmountMicros, currencyCode)
}

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
        // Amazon sends a nbsp character in countries with euros "5,80 €"
        // Android devices will match the nbsp, JVM (when running on unit tests will not match the nbsp)
        // So we remove them and trim just in case
        var price =
            dirtyPrice.replace(" ", "")
                .replace(" ", "") // This is a NBSP, some editors might render it as a space or a tab
                .replace("${Typography.nbsp}", "")
                .trim()
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
