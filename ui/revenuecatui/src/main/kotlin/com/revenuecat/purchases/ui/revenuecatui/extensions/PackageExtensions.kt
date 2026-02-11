package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

public val Package.isSubscription: Boolean
    get() = product.type == ProductType.SUBS

public val Package.isMonthly: Boolean
    get() = product.period?.let { it.unit == Period.Unit.MONTH && it.value == 1 } ?: false

/**
 * Calculates offer eligibility with fallback logic:
 * 1. If a promo offer is configured and has discount phases, use promo eligibility
 * 2. If promo offer is ineligible (no discount phases), fall back to intro offer eligibility
 * 3. If no intro offer, return Ineligible
 */
internal fun calculateOfferEligibility(resolvedOffer: ResolvedOffer?, rcPackage: Package): OfferEligibility {
    if (resolvedOffer != null && resolvedOffer.isPromoOffer) {
        val promoEligibility = resolvedOffer.promoOfferEligibility()
        if (promoEligibility != OfferEligibility.PromoOfferIneligible) {
            return promoEligibility
        }
    }
    return rcPackage.introOfferEligibility
}

internal val Package.introOfferEligibility: OfferEligibility
    get() {
        val phaseCount = (product.defaultOption?.pricingPhases?.size ?: 0) - 1

        return when (phaseCount) {
            1 -> OfferEligibility.IntroOfferSingle
            2 -> OfferEligibility.IntroOfferMultiple
            else -> OfferEligibility.Ineligible
        }
    }

private fun ResolvedOffer.promoOfferEligibility(): OfferEligibility {
    val offerPhaseCount = (subscriptionOption?.pricingPhases?.size ?: 0) - 1

    return when (offerPhaseCount) {
        1 -> OfferEligibility.PromoOfferSingle
        2 -> OfferEligibility.PromoOfferMultiple
        else -> OfferEligibility.PromoOfferIneligible
    }
}

internal val TemplateConfiguration.PackageInfo.offerEligibility: OfferEligibility
    get() = this.rcPackage.introOfferEligibility
