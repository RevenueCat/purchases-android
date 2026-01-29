package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

val Package.isSubscription: Boolean
    get() = product.type == ProductType.SUBS

val Package.isMonthly: Boolean
    get() = product.period?.let { it.unit == Period.Unit.MONTH && it.value == 1 } ?: false

/**
 * Calculates offer eligibility based on the subscription option's phases.
 *
 * @param isPromoOffer Whether this is a configured promo offer (vs default option).
 */
internal fun SubscriptionOption.toOfferEligibility(isPromoOffer: Boolean): OfferEligibility {
    val phaseCount = when {
        isBasePlan -> 0
        (freePhase != null && introPhase == null) ||
            (freePhase == null && introPhase != null) -> 1
        freePhase != null && introPhase != null -> 2
        else -> 0
    }

    return if (isPromoOffer) {
        when (phaseCount) {
            1 -> OfferEligibility.PromoOfferSingle
            2 -> OfferEligibility.PromoOfferMultiple
            else -> OfferEligibility.PromoOfferIneligible
        }
    } else {
        when (phaseCount) {
            1 -> OfferEligibility.IntroOfferSingle
            2 -> OfferEligibility.IntroOfferMultiple
            else -> OfferEligibility.Ineligible
        }
    }
}

/**
 * Calculates offer eligibility based on the package's default option.
 * Always returns an IntroOffer* or Ineligible variant (never PromoOffer*).
 */
internal val Package.offerEligibility: OfferEligibility
    get() = product.defaultOption?.toOfferEligibility(isPromoOffer = false) ?: OfferEligibility.Ineligible

/**
 * Calculates offer eligibility based on the resolved offer.
 * Returns PromoOffer* variants if a promo offer is configured, otherwise IntroOffer* variants.
 */
internal val ResolvedOffer.offerEligibility: OfferEligibility
    get() = subscriptionOption?.toOfferEligibility(isPromoOffer) ?: OfferEligibility.Ineligible

internal val TemplateConfiguration.PackageInfo.offerEligibility: OfferEligibility
    get() = this.rcPackage.offerEligibility
