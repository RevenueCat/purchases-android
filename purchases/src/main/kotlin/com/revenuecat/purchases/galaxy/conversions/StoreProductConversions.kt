package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyPurchasingData
import com.revenuecat.purchases.galaxy.GalaxyStoreProduct
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.GalaxySubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo
import java.util.HashSet

private const val ELIGIBILITY_PRICING_FREE_TRIAL = "FreeTrial"
private const val ELIGIBILITY_PRICING_TIERED_PRICE = "TieredPrice"

internal fun ProductVo.toStoreProduct(
    promotionEligibilities: List<PromotionEligibilityVo>? = null,
): StoreProduct {
    val productType = this.type.createRevenueCatProductTypeFromSamsungIAPTypeString()
    val productId = this.itemId
    val standardPrice = this.createPrice()
    val standardBillingPeriod: Period? = if (productType == ProductType.SUBS) {
        val period = this.createPeriod()
        if (period == null) {
            log(LogIntent.GALAXY_WARNING) {
                GalaxyStrings.CANNOT_PARSE_GALAXY_PRODUCT_SUBSCRIPTION_PERIOD
                    .format(this.subscriptionDurationMultiplier)
            }
        }
        period
    } else {
        null
    }

    val subscriptionOptions: SubscriptionOptions?
    val defaultOption: SubscriptionOption?
    if (productType == ProductType.SUBS && standardBillingPeriod != null) {
        val subscriptionOption = GalaxySubscriptionOption(
            id = this.itemId,
            pricingPhases = this.createPricingPhases(
                promotionEligibilities = promotionEligibilities,
                standardBillingPeriod = standardBillingPeriod,
                standardPrice = standardPrice,
            ),
            tags = emptyList(), // Tags are unsupported on the Galaxy Store
            presentedOfferingContext = null,
            purchasingData = GalaxyPurchasingData.Product(productId = productId, productType = productType),
            installmentsInfo = null
        )

        subscriptionOptions = SubscriptionOptions(subscriptionOptions = listOf(subscriptionOption))
        defaultOption = subscriptionOption
    } else {
        subscriptionOptions = null
        defaultOption = null
    }

    return GalaxyStoreProduct(
        id = productId,
        type = productType,
        price = standardPrice,
        name = this.itemName,
        title = this.itemName,
        description = this.itemDesc,
        period = standardBillingPeriod,
        subscriptionOptions = subscriptionOptions,
        defaultOption = defaultOption,
        presentedOfferingContext = null,
    )
}

private fun ProductVo.createPricingPhases(
    promotionEligibilities: List<PromotionEligibilityVo>?,
    standardBillingPeriod: Period,
    standardPrice: Price,
): List<PricingPhase> {
    val pricingPhases: MutableList<PricingPhase> = mutableListOf()
    val type = this.type.createRevenueCatProductTypeFromSamsungIAPTypeString()

    val eligibilityPricings: MutableSet<String> = HashSet()
    promotionEligibilities
        ?.filter { it.itemId == this.itemId }
        ?.mapTo(eligibilityPricings) { it.pricing }

    if (
        eligibilityPricings.contains(ELIGIBILITY_PRICING_FREE_TRIAL) &&
        eligibilityPricings.contains(ELIGIBILITY_PRICING_TIERED_PRICE)
    ) {
        // We are temporarily not handling Tiered Pricing price phases when there is both a trial and a tiered
        // price available
        log(LogIntent.GALAXY_WARNING) {
            GalaxyStrings.PARSING_INTRO_PRICING_PHASES_FOR_SUBS_TIERED_PRICING_NOT_SUPPORTED
        }
        this.createFreeTrialPricingPhase()?.let { pricingPhases.addFirst(it) }
    } else if (eligibilityPricings.contains(ELIGIBILITY_PRICING_FREE_TRIAL)) {
        this.createFreeTrialPricingPhase()?.let { pricingPhases.addFirst(it) }
    } else if (eligibilityPricings.contains(ELIGIBILITY_PRICING_TIERED_PRICE)) {
        // We are temporarily ignoring lower tier subscriptions
        log(LogIntent.GALAXY_WARNING) {
            GalaxyStrings.PARSING_INTRO_PRICING_PHASES_FOR_SUBS_TIERED_PRICING_NOT_SUPPORTED
        }
    }

    val normalPricingPhase = PricingPhase(
        billingPeriod = standardBillingPeriod,
        recurrenceMode = if(type == ProductType.SUBS) {
            RecurrenceMode.INFINITE_RECURRING
        } else {
            RecurrenceMode.NON_RECURRING
        },
        billingCycleCount = null,
        price = standardPrice
    )
    pricingPhases.addLast(normalPricingPhase)
    return pricingPhases
}

private fun ProductVo.createFreeTrialPricingPhase(): PricingPhase? {
    return this.freeTrialPeriod.toIntOrNull()?.let { freeTrialInDays ->
        PricingPhase(
            billingPeriod = Period(
                value = freeTrialInDays,
                unit = Period.Unit.DAY,
                iso8601 = "P${freeTrialInDays}D"
            ),
            recurrenceMode = RecurrenceMode.NON_RECURRING,
            billingCycleCount = null,
            price = Price(
                formatted = "%s%.2f".format(currencyUnit, 0.0),
                amountMicros = 0L,
                currencyCode = currencyCode,
            ),
        )
    } // This returns null if the string couldn't be parsed to an Int
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

internal fun String.createRevenueCatProductTypeFromSamsungIAPTypeString(): ProductType {
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
