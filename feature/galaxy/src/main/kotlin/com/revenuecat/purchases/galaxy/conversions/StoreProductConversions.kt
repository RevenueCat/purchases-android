package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.GalaxyPurchasingData
import com.revenuecat.purchases.galaxy.GalaxyStoreProduct
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.GalaxySubscriptionOption
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
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
            installmentsInfo = null,
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
        this.createFreeTrialPricingPhase()?.let { pricingPhases.add(it) }
        this.createTieredSubscriptionPricingPhase()?.let { pricingPhases.add(it) }
    } else if (eligibilityPricings.contains(ELIGIBILITY_PRICING_FREE_TRIAL)) {
        this.createFreeTrialPricingPhase()?.let { pricingPhases.add(it) }

        if (this.hasTieredSubscription()) {
            // When a Galaxy product has both a trial and a tiered subscription, and the user is eligible for both,
            // IapHelper.getPromotionEligibility only returns a value stating that the user is eligible for the trial.
            // However, when the user proceeds to purchase the option, both the trial and tiered subscription are
            // applied to the purchase (trial first, then tiered subscription). Due to this quirk in the IapHelper's
            // APIs, we make an assumption that if the product has a tiered subscription, and the user is eligible
            // for the product's trial, that the user is also eligible for the tiered subscription, since you must
            // redeem them both together at once.
            this.createTieredSubscriptionPricingPhase()?.let { pricingPhases.add(it) }
        }
    } else if (eligibilityPricings.contains(ELIGIBILITY_PRICING_TIERED_PRICE)) {
        this.createTieredSubscriptionPricingPhase()?.let { pricingPhases.add(it) }
    }

    val normalPricingPhase = PricingPhase(
        billingPeriod = standardBillingPeriod,
        recurrenceMode = if (type == ProductType.SUBS) {
            RecurrenceMode.INFINITE_RECURRING
        } else {
            RecurrenceMode.NON_RECURRING
        },
        billingCycleCount = null,
        price = standardPrice,
    )
    pricingPhases.add(normalPricingPhase)
    return pricingPhases
}

private fun ProductVo.createFreeTrialPricingPhase(): PricingPhase? {
    // Free trials are always measured in days for Galaxy Products
    return this.freeTrialPeriod.toIntOrNull()?.let { freeTrialInDays ->
        PricingPhase(
            billingPeriod = Period(
                value = freeTrialInDays,
                unit = Period.Unit.DAY,
                iso8601 = "P${freeTrialInDays}D",
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

@SuppressWarnings("ReturnCount")
private fun ProductVo.createTieredSubscriptionPricingPhase(): PricingPhase? {
    if (!this.hasTieredSubscription()) { return null }
    val tieredPrice = this.tieredPrice.toDoubleOrNull() ?: return null

    // tieredSubscriptionCount: If a tiered subscription is available, the number of lower-tier subscription periods
    val tieredSubscriptionCount = this.tieredSubscriptionCount.toIntOrNull() ?: return null
    val billingPeriod = createPeriodFromGalaxyData(
        durationMultiplier = this.tieredSubscriptionDurationMultiplier,
        durationUnit = this.tieredSubscriptionDurationUnit,
    ) ?: return null

    return PricingPhase(
        billingPeriod = billingPeriod,
        recurrenceMode = RecurrenceMode.FINITE_RECURRING,
        billingCycleCount = tieredSubscriptionCount,
        price = createPriceFromGalaxyData(
            currencyCode = this.currencyCode,
            itemPrice = tieredPrice,
            formattedString = this.tieredPriceString,
        ),
    )
}

private fun ProductVo.createPrice(): Price = createPriceFromGalaxyData(
    currencyCode = this.currencyCode,
    itemPrice = this.itemPrice,
    formattedString = this.itemPriceString,
)

private fun ProductVo.hasTieredSubscription(): Boolean {
    // ProductVo.tieredSubscriptionYN: For subscriptions only, whether or not the subscription has two-tiered pricing
    // "Y": The subscription has one or more lower-price subscription periods followed by regular-price periods
    // "N": The subscription only has regular-price subscription periods
    return this.tieredSubscriptionYN == "Y"
}

private fun createPriceFromGalaxyData(
    currencyCode: String,
    itemPrice: Double,
    formattedString: String,
): Price {
    return Price(
        formatted = formattedString,
        amountMicros = (itemPrice * 1_000_000L).toLong(),
        currencyCode = currencyCode,
    )
}

@SuppressWarnings("MagicNumber", "ReturnCount")
private fun ProductVo.createPeriod(): Period? = createPeriodFromGalaxyData(
    durationMultiplier = this.subscriptionDurationMultiplier,
    durationUnit = this.subscriptionDurationUnit,
)

@SuppressWarnings("ReturnCount")
private fun createPeriodFromGalaxyData(
    durationMultiplier: String,
    durationUnit: String,
): Period? {
    // subscriptionDurationMultiplier returns a string in the format $INT$STRING, like
    // 1YEAR, 2MONTH, 4WEEK. We need to extract that leading integer to use as the
    // period's value.
    fun extractLeadingInt(input: String): Int? {
        // A regular expression that matches one or more digits (\d+)
        // at the beginning of the string (^).
        val regex = "^\\d+".toRegex()

        // Find the first match of the regex in the input string.
        val matchResult = regex.find(input)

        // If a match is found, convert the matched string (value) to an Int.
        // If no match is found (e.g., input is "MONTH2"), it returns null.
        return matchResult?.value?.toIntOrNull()
    }

    val periodValue = extractLeadingInt(input = durationMultiplier)
    if (periodValue == null) {
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.CANNOT_PARSE_LEADING_INT_FROM_SUBSCRIPTION_DURATION_MULTIPLIER
                .format(durationMultiplier)
        }
        return null
    }
    val unit = durationUnit.createRevenueCatUnitFromSamsungIAPSubscriptionDurationUnitString()
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
