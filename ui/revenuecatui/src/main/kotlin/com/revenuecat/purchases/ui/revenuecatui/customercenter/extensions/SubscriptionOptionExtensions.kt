@file:JvmSynthetic
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
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

private fun PricingPhase.localizedDuration(
    locale: Locale,
): String {
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).format(
        Measure(billingCycleCount, billingPeriod.unit.measureUnit),
    )
}

private fun SubscriptionOption.getTwoPhaseDescription(
    localization: CustomerCenterConfigData.Localization,
    locale: Locale,
): String {
    val phase = pricingPhases.first()
    val duration = phase.localizedDuration(locale)
    val fullPricePhase = this.pricingPhases.last()
    val basePrice = fullPricePhase.price.localizedPerPeriod(
        fullPricePhase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    val replacements = mapOf(
        CustomerCenterConfigData.Localization.VariableName.SUB_OFFER_DURATION to duration,
        CustomerCenterConfigData.Localization.VariableName.SUB_OFFER_PRICE to phase.price.formatted,
        CustomerCenterConfigData.Localization.VariableName.PRICE to basePrice,
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
            // "$0.99 during 2 months, then $3.99/mth"
            val commonLocalizedString =
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DISCOUNTED_RECURRING_THEN_PRICE,
                )
            replaceVariables(commonLocalizedString, replacements)
        }

        else -> basePrice
    }
}

private fun SubscriptionOption.getThreePhaseDescription(
    localization: CustomerCenterConfigData.Localization,
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

    val trialDuration = firstPhase.localizedDuration(locale)
    val secondDuration = secondPhase.localizedDuration(locale)

    val replacements = mapOf(
        CustomerCenterConfigData.Localization.VariableName.SUB_OFFER_DURATION to trialDuration,
        CustomerCenterConfigData.Localization.VariableName.SUB_OFFER_DURATION_2 to secondDuration,
        CustomerCenterConfigData.Localization.VariableName.SUB_OFFER_PRICE_2 to secondPhase.price.formatted,
        CustomerCenterConfigData.Localization.VariableName.PRICE to basePrice,
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
            val commonLocalizedString =
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_DISCOUNTED_THEN_PRICE,
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
