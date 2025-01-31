package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.content.Context
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.utils.getDefaultLocales
import java.util.Locale

internal fun SubscriptionOption.getLocalizedDescription(context: Context): String {
    val locale = getDefaultLocales().first()

    return when (pricingPhases.size) {
        // Note last pricing phase is the base subscription price
        2 -> this.getTwoPhaseDescription(context, locale)
        3 -> this.getThreePhaseDescription(context, locale)
        else -> pricingPhases.first().price.formatted // Fallback for unexpected number of phases
    }
}

private fun SubscriptionOption.getTwoPhaseDescription(context: Context, locale: Locale): String {
    val phase = pricingPhases.first()
    val period = phase.billingPeriod.localizedPeriod(locale)
    val fullPricePhase = this.pricingPhases.last()
    val basePrice = fullPricePhase.price.localizedPerPeriod(
        fullPricePhase.billingPeriod,
        locale,
        showZeroDecimalPlacePrices = false,
    )

    return when (phase.offerPaymentMode) {
        OfferPaymentMode.FREE_TRIAL -> {
            context.getString(
                R.string.free_trial_then_price, // "First %s free, then %s"
                period,
                basePrice,
            )
        }
        OfferPaymentMode.SINGLE_PAYMENT -> {
            context.getString(
                R.string.single_payment_then_price, // "%s for %s, then %s"
                period,
                phase.price.formatted,
                basePrice,
            )
        }
        OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT -> {
            context.getString(
                R.string.discounted_recurring_then_price, // "%s/mo during %s, then %s"
                phase.price.formatted,
                period,
                basePrice,
            )
        }
        else -> phase.price.formatted
    }
}

private fun SubscriptionOption.getThreePhaseDescription(context: Context, locale: Locale): String {
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
        return this.getTwoPhaseDescription(context, locale)
    }

    val trialPeriod = firstPhase.billingPeriod.localizedPeriod(locale)
    val secondPeriod = secondPhase.billingPeriod.localizedPeriod(locale)

    return when (secondPhase.offerPaymentMode) {
        OfferPaymentMode.SINGLE_PAYMENT -> {
            context.getString(
                R.string.free_trial_single_payment_then_price, // "Try %s for free, then %s for %s, and %s thereafter"
                trialPeriod,
                secondPhase.price.formatted,
                secondPeriod,
                basePrice,
            )
        }
        OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT -> {
            context.getString(
                R.string.free_trial_discounted_then_price, // "Try %s for free, then %s for your first %s, and %s thereafter"
                trialPeriod,
                secondPhase.price.formatted,
                secondPeriod,
                basePrice,
            )
        }
        else -> this.getTwoPhaseDescription(context, locale)
    }
}
