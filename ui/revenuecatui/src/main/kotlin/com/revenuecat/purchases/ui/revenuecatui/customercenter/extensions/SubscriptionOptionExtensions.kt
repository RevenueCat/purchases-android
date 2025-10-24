@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedPerPeriod
import com.revenuecat.purchases.ui.revenuecatui.extensions.measureUnit
import java.util.Locale

@JvmSynthetic
internal fun SubscriptionOption.getLocalizedDescription(
    localization: CustomerCenterConfigData.Localization,
    locale: Locale,
): String {
    @Suppress("MagicNumber")
    return when (pricingPhases.size) {
        // Note last pricing phase is the base subscription price
        2 -> this.getTwoPhaseDescription(localization, locale)
        3 -> this.getThreePhaseDescription(localization, locale)
        else -> pricingPhases.first().price.formatted // Fallback for unexpected number of phases
    }
}

private fun PricingPhase.localizedTotalDuration(
    locale: Locale,
): String {
    val duration = (billingCycleCount ?: 1) * billingPeriod.value
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).format(
        Measure(duration, billingPeriod.unit.measureUnit),
    )
}

private fun SubscriptionOption.getTwoPhaseDescription(
    localization: CustomerCenterConfigData.Localization,
    locale: Locale,
): String {
    val phase = pricingPhases.first()
    val duration = phase.localizedTotalDuration(locale)
    val fullPricePhase = this.pricingPhases.last()
    val basePrice = fullPricePhase.price.localizedPerPeriod(
        fullPricePhase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    val billingCycleCount = phase.billingCycleCount ?: 1
    val pricePerPeriod = phase.price.localizedPerPeriod(
        phase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    val replacements = mapOf(
        Localization.VariableName.SUB_OFFER_DURATION to duration,
        Localization.VariableName.SUB_OFFER_PRICE to phase.price.formatted,
        Localization.VariableName.PRICE to basePrice,
        Localization.VariableName.DISCOUNTED_RECURRING_PAYMENT_PRICE_PER_PERIOD to pricePerPeriod,
        Localization.VariableName.DISCOUNTED_RECURRING_PAYMENT_CYCLES to billingCycleCount.toString(),
    )

    return when (phase.offerPaymentMode) {
        OfferPaymentMode.FREE_TRIAL -> {
            // "First 2 months free, then $3.99/mth"
            val commonLocalizedString =
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        OfferPaymentMode.SINGLE_PAYMENT -> {
            // "2 months for $0.99, then $3.99/mth"
            val commonLocalizedString =
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.SINGLE_PAYMENT_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT -> {
            // "$0.99/mth for 2 periods, then $3.99/mth" (always billingCycleCount >= 2)
            val commonLocalizedString =
                localization.commonLocalizedString(
                    Localization.CommonLocalizedString.DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        else -> basePrice
    }
}

private fun SubscriptionOption.getThreePhaseDescription(
    localization: Localization,
    locale: Locale,
): String {
    val firstPhase = pricingPhases.first()
    val secondPhase = pricingPhases[1]
    val fullPricePhase = pricingPhases.last()
    val basePrice = fullPricePhase.price.localizedPerPeriod(
        fullPricePhase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    // First phase must be free trial
    if (firstPhase.offerPaymentMode != OfferPaymentMode.FREE_TRIAL) {
        return this.getTwoPhaseDescription(localization, locale)
    }

    val trialDuration = firstPhase.localizedTotalDuration(locale)
    val secondDuration = secondPhase.localizedTotalDuration(locale)

    // For discounted recurring, add billing period and cycle info
    val secondBillingCycleCount = secondPhase.billingCycleCount ?: 1
    val secondPricePerPeriod = secondPhase.price.localizedPerPeriod(
        secondPhase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    val replacements = mapOf(
        Localization.VariableName.SUB_OFFER_DURATION to trialDuration,
        Localization.VariableName.SUB_OFFER_DURATION_2 to secondDuration,
        Localization.VariableName.SUB_OFFER_PRICE_2 to secondPhase.price.formatted,
        Localization.VariableName.DISCOUNTED_RECURRING_PAYMENT_PRICE_PER_PERIOD to secondPricePerPeriod,
        Localization.VariableName.PRICE to basePrice,
        Localization.VariableName.DISCOUNTED_RECURRING_PAYMENT_CYCLES to secondBillingCycleCount.toString(),
    )

    return when (secondPhase.offerPaymentMode) {
        OfferPaymentMode.SINGLE_PAYMENT -> {
            val commonLocalizedString =
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT -> {
            // "Try X for free, then $0.99/mth for 2 periods, and $3.99/mth thereafter" (always billingCycleCount >= 2)
            val commonLocalizedString =
                localization.commonLocalizedString(
                    Localization.CommonLocalizedString.FREE_TRIAL_DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        else -> basePrice
    }
}

private fun replaceVariables(
    template: String,
    replacements: Map<CustomerCenterConfigData.Localization.VariableName, String>,
): String {
    val regex = Regex("\\{\\{\\s*([^}]+)\\s*\\}\\}")
    return regex.replace(template) { matchResult ->
        val variable = matchResult.groupValues[1].trim()
        replacements.entries.firstOrNull { it.key.identifier == variable }?.value ?: matchResult.value
    }
}
