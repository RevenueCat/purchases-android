package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer

val Package.isSubscription: Boolean
    get() = product.type == ProductType.SUBS

val Package.isMonthly: Boolean
    get() = product.period?.let { it.unit == Period.Unit.MONTH && it.value == 1 } ?: false

/**
 * Calculates intro offer eligibility based on the subscription option's phases.
 */
internal val SubscriptionOption.introEligibility: IntroOfferEligibility
    get() = when {
        isBasePlan -> IntroOfferEligibility.INELIGIBLE
        (freePhase != null && introPhase == null) ||
            (freePhase == null && introPhase != null) ->
            IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE
        freePhase != null && introPhase != null -> IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE
        else -> IntroOfferEligibility.INELIGIBLE
    }

/**
 * Calculates intro offer eligibility based on the package's default option.
 */
internal val Package.introEligibility: IntroOfferEligibility
    get() = product.defaultOption?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

/**
 * Calculates intro offer eligibility based on the resolved offer.
 * If a promo offer is configured, uses that option's phases.
 * Otherwise, uses the default option.
 */
internal val ResolvedOffer.introEligibility: IntroOfferEligibility
    get() = subscriptionOption?.introEligibility ?: IntroOfferEligibility.INELIGIBLE

internal val TemplateConfiguration.PackageInfo.introEligibility: IntroOfferEligibility
    get() = this.rcPackage.introEligibility
