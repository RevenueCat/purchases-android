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

internal fun discountPhases(subscriptionOption: SubscriptionOption?, rcPackage: Package?): List<PricingPhase> {
    val option = subscriptionOption ?: rcPackage?.product?.defaultOption
    return option?.pricingPhases?.dropLast(1).orEmpty()
}

internal fun resolveOfferPrice(
    discountPhase: PricingPhase?,
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
    productFallback: () -> String?,
): String? =
    discountPhase?.productOfferPrice(locale, localizedVariableKeys, freeTrialRendering) ?: productFallback()

internal fun resolveOfferPeriod(
    discountPhase: PricingPhase?,
    discountValue: (PricingPhase) -> String?,
    productFallback: () -> String?,
): String? =
    discountPhase?.let(discountValue) ?: productFallback()

/**
 * Whether a zero priced offer shows the localized word ("Free") or the formatted amount ("$0.00").
 */
internal enum class FreeTrialRendering {
    WORD,
    AMOUNT,
}

internal fun PricingPhase.productOfferPrice(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
): String? =
    if (freeTrialRendering == FreeTrialRendering.WORD && price.amountMicros == 0L) {
        localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
    } else {
        price.getFormatted(locale)
    }

internal fun PricingPhase.productOfferPricePerDay(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
): String? =
    productOfferPricePerPeriod(locale, localizedVariableKeys, Period.Unit.DAY, freeTrialRendering) {
        pricePerDay(locale)
    }

internal fun PricingPhase.productOfferPricePerWeek(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
): String? =
    productOfferPricePerPeriod(locale, localizedVariableKeys, Period.Unit.WEEK, freeTrialRendering) {
        pricePerWeek(locale)
    }

internal fun PricingPhase.productOfferPricePerMonth(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
): String? =
    productOfferPricePerPeriod(locale, localizedVariableKeys, Period.Unit.MONTH, freeTrialRendering) {
        pricePerMonth(locale)
    }

internal fun PricingPhase.productOfferPricePerYear(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    freeTrialRendering: FreeTrialRendering,
): String? =
    productOfferPricePerPeriod(locale, localizedVariableKeys, Period.Unit.YEAR, freeTrialRendering) {
        pricePerYear(locale)
    }

internal fun PricingPhase.productOfferPricePerPeriod(
    locale: Locale,
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    unit: Period.Unit,
    freeTrialRendering: FreeTrialRendering,
    calculatePrice: PricingPhase.() -> Price,
): String? =
    takeIf { it.canDisplay(unit) }
        ?.calculatePrice()
        ?.let { offerPrice ->
            if (freeTrialRendering == FreeTrialRendering.WORD && offerPrice.amountMicros == 0L) {
                localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
            } else {
                offerPrice.getFormatted(locale)
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

internal fun PricingPhase.productOfferPeriodInDays(locale: Locale): String? =
    productOfferPeriodInPeriodUnit(Period.Unit.DAY) { roundedValueInDays(locale) }

internal fun PricingPhase.productOfferPeriodInWeeks(locale: Locale): String? =
    productOfferPeriodInPeriodUnit(Period.Unit.WEEK) { roundedValueInWeeks(locale) }

internal fun PricingPhase.productOfferPeriodInMonths(locale: Locale): String? =
    productOfferPeriodInPeriodUnit(Period.Unit.MONTH) { roundedValueInMonths(locale) }

internal fun PricingPhase.productOfferPeriodInYears(locale: Locale): String? =
    productOfferPeriodInPeriodUnit(Period.Unit.YEAR) { roundedValueInYears(locale) }

internal fun PricingPhase.productOfferPeriodWithUnit(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
    locale: Locale,
): String? =
    localizedVariableKeys
        .getStringOrLogError(billingPeriod.periodValueWithUnitLocalizationKey)
        ?.format(locale, billingPeriod.value)

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
