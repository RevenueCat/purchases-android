@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal fun primaryDiscountPhase(subscriptionOption: SubscriptionOption?, rcPackage: Package?): PricingPhase? {
    val option = subscriptionOption ?: rcPackage?.product?.defaultOption
    return option?.let { it.freePhase ?: it.introPhase }
}

internal fun secondaryDiscountPhase(subscriptionOption: SubscriptionOption?, rcPackage: Package?): PricingPhase? {
    val option = subscriptionOption ?: rcPackage?.product?.defaultOption
    return option?.let { if (it.freePhase != null) it.introPhase else null }
}

internal fun PricingPhase.productOfferPrice(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    if (price.amountMicros == 0L) {
        localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
    } else {
        price.formatted
    }

internal fun PricingPhase.productOfferPricePerDay(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.DAY) { pricePerDay(locale) }

internal fun PricingPhase.productOfferPricePerWeek(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.WEEK) { pricePerWeek(locale) }

internal fun PricingPhase.productOfferPricePerMonth(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.MONTH) { pricePerMonth(locale) }

internal fun PricingPhase.productOfferPricePerYear(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.YEAR) { pricePerYear(locale) }

internal fun PricingPhase.productOfferPricePerPeriod(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    unit: Period.Unit,
    calculatePrice: PricingPhase.() -> Price,
): String? =
    takeIf { it.canDisplay(unit) }
        ?.calculatePrice()
        ?.let { offerPrice ->
            if (offerPrice.amountMicros == 0L) {
                localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
            } else {
                offerPrice.formatted
            }
        }

internal fun PricingPhase.productOfferPeriod(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? = billingPeriod.periodUnitLocalizationKey?.let { key ->
    localizedVariableKeys.getStringOrLogError(key)
}

internal fun PricingPhase.productOfferPeriodAbbreviated(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? = billingPeriod.periodUnitAbbreviatedLocalizationKey?.let { key ->
    localizedVariableKeys.getStringOrLogError(key)
}

internal fun PricingPhase.productOfferPeriodInPeriodUnit(
    unit: Period.Unit,
    calculateValue: Period.() -> String,
): String? =
    takeIf { it.canDisplay(unit) }?.billingPeriod?.calculateValue()

internal val PricingPhase.productOfferPeriodInDays: String?
    get() = productOfferPeriodInPeriodUnit(Period.Unit.DAY) { roundedValueInDays }

internal val PricingPhase.productOfferPeriodInWeeks: String?
    get() = productOfferPeriodInPeriodUnit(Period.Unit.WEEK) { roundedValueInWeeks }

internal val PricingPhase.productOfferPeriodInMonths: String?
    get() = productOfferPeriodInPeriodUnit(Period.Unit.MONTH) { roundedValueInMonths }

internal val PricingPhase.productOfferPeriodInYears: String?
    get() = productOfferPeriodInPeriodUnit(Period.Unit.YEAR) { roundedValueInYears }

internal fun PricingPhase.productOfferPeriodWithUnit(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    localizedVariableKeys
        .getStringOrLogError(billingPeriod.periodValueWithUnitLocalizationKey)
        ?.format(billingPeriod.value)

internal fun PricingPhase.productOfferEndDate(locale: Locale, date: Date): String? {
    val futureDate = Calendar.getInstance(locale)
        .apply { time = date }
        .apply { add(Calendar.DAY_OF_YEAR, billingPeriod.valueInDays.roundToInt()) }
        .time

    return DateFormat.getDateInstance(DateFormat.LONG, locale)
        .format(futureDate)
}

internal fun PricingPhase.canDisplay(unit: Period.Unit): Boolean =
    unit.ordinal <= billingPeriod.unit.ordinal
