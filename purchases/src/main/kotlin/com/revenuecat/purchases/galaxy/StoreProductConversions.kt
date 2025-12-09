package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.vo.ProductVo

internal fun ProductVo.toStoreProduct(): StoreProduct {
    val type = this.type.createRevenueCatProductTypeFromSamsungIAPTypeString()
    val period: Period? = if (type == ProductType.SUBS) {
        this.createPeriod()
    } else {
        null
    }

    return GalaxyStoreProduct(
        id = this.itemId,
        type = type,
        price = this.createPrice(),
        name = this.itemName,
        title = this.itemName,
        description = this.itemDesc,
        period = period,
        subscriptionOptions = null,
        defaultOption = null,
        presentedOfferingContext = null,
    )
}

private fun ProductVo.createPrice(): Price =
    Price(
        // Here, we manually build the formatted string instead of using ProductVo.itemPriceString
        // because itemPriceString doesn't include the decimal values if the amount is an integer with no decimal value.
        // This way, we can get strings like "$3.00" instead of "$3"
        formatted = "%s%.2f".format(currencyUnit, itemPrice),
        amountMicros = (itemPrice * 1_000_000L).toLong(),
        currencyCode = currencyCode,
    )

@SuppressWarnings("MagicNumber", "ReturnCount")
private fun ProductVo.createPeriod(): Period? {
    // subscriptionDurationMultiplier returns a string in the format $INT$STRING, like
    // 1YEAR, 2MONTH, 4WEEK. We need to extract that leading integer to use as the
    // period's value.
    val periodValue = extractLeadingInt(input = this.subscriptionDurationMultiplier)
    if (periodValue == null) {
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.CANNOT_PARSE_LEADING_INT_FROM_SUBSCRIPTION_DURATION_MULTIPLIER
                .format(this.subscriptionDurationMultiplier)
        }
        return null
    }
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
        iso8601 = "P${periodValue}$isoUnit",
    )
}

private fun String.createRevenueCatUnitFromSamsungIAPSubscriptionDurationUnitString(): Period.Unit? {
    // Valid values are YEAR, MONTH, and WEEK
    // https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html#Get-in-app-product-details
    return when (this.lowercase()) {
        "year" -> Period.Unit.YEAR
        "month" -> Period.Unit.MONTH
        "week" -> Period.Unit.WEEK
        else -> {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.UNKNOWN_SUBSCRIPTION_DURATION_UNIT.format(this)
            }
            Period.Unit.UNKNOWN
        }
    }
}

private fun extractLeadingInt(input: String): Int? {
    // A regular expression that matches one or more digits (\d+)
    // at the beginning of the string (^).
    val regex = "^\\d+".toRegex()

    // Find the first match of the regex in the input string.
    val matchResult = regex.find(input)

    // If a match is found, convert the matched string (value) to an Int.
    // If no match is found (e.g., input is "MONTH2"), it returns null.
    return matchResult?.value?.toIntOrNull()
}

private fun String.createRevenueCatProductTypeFromSamsungIAPTypeString(): ProductType {
    return when (this.lowercase()) {
        "item" -> ProductType.INAPP
        "subscription" -> ProductType.SUBS
        else -> {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.UNKNOWN_GALAXY_IAP_TYPE_STRING.format(this)
            }
            ProductType.UNKNOWN
        }
    }
}
