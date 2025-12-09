package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.vo.ProductVo

internal fun ProductVo.toStoreProduct(): StoreProduct {
    return GalaxyStoreProduct(
        id = this.itemId,
        type = this.type.createRevenueCatProductTypeFromSamsungIAPTypeString(),
        price = this.createPrice(),
        name = this.itemName,
        title = this.itemName,
        description = this.itemDesc,
        period = this.createPeriod(),
        subscriptionOptions = null,
        defaultOption = null,
        presentedOfferingContext = null
    )
}

private fun ProductVo.createPrice(): Price =
    Price(
        // Here, we manually build the formatted string instead of using ProductVo.itemPriceString
        // because itemPriceString doesn't include the decimal values if the amount is an integer with no decimal value.
        // This way, we can get strings like "$3.00" instead of "$3"
        formatted = "%s%.2f".format(currencyUnit, itemPrice),
        amountMicros = (itemPrice * 1_000_000L).toLong(),
        currencyCode = currencyCode
    )

@SuppressWarnings("MagicNumber")
private fun ProductVo.createPeriod(): Period? {
    // subscriptionDurationMultiplier returns a string in the format $INT$STRING, like
    // 1YEAR, 2MONTH, 4WEEK. We need to extract that leading integer to use as the
    // period's value.
    val periodValue = extractLeadingInt(input = this.subscriptionDurationMultiplier) ?: return null
    val unit = this.subscriptionDurationUnit.createRevenueCatUnitFromSamsungIAPSubscriptionDurationUnitString()
        ?: return null

    val isoUnit = when (unit) {
        Period.Unit.DAY -> "D"
        Period.Unit.WEEK -> "W"
        Period.Unit.MONTH -> "M"
        Period.Unit.YEAR -> "Y"
        Period.Unit.UNKNOWN -> return null
    }

    return Period(
        value = periodValue,
        unit = unit,
        iso8601 = "P${periodValue}${isoUnit}"
    )
}

private fun String.createRevenueCatUnitFromSamsungIAPSubscriptionDurationUnitString(): Period.Unit? {
    // Valid values are YEAR, MONTH, and WEEK
    // https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html#Get-in-app-product-details
    return when(this.lowercase()) {
        "year" -> Period.Unit.YEAR
        "month" -> Period.Unit.MONTH
        "week" -> Period.Unit.WEEK
        else -> {
            // TODO: Log
            null
        }
    }
}

//internal String

private fun extractLeadingInt(input: String): Int? {
    // A regular expression that matches one or more digits (\d+)
    // at the beginning of the string (^).
    val regex = "^\\d+".toRegex()

    // Find the first match of the regex in the input string.
    val matchResult = regex.find(input)

    // If a match is found, convert the matched string (value) to an Int.
    // If no match is found (e.g., input is "MONTH2"), it returns null.
    val value = matchResult?.value?.toIntOrNull()
    if (value == null)  {
        // TODO: Log
    }
    return value
}

private fun String.createRevenueCatProductTypeFromSamsungIAPTypeString(): ProductType {
    return when(this.lowercase()) {
        "item" -> ProductType.INAPP
        "subscription" -> ProductType.SUBS
        else -> {
            // TODO: Log
            ProductType.UNKNOWN
        }
    }
}
