package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration

val Package.isSubscription: Boolean
    get() = product.type == ProductType.SUBS

val Package.isMonthly: Boolean
    get() = product.period?.let { it.unit == Period.Unit.MONTH && it.value == 1 } ?: false

internal val Package.introEligibility: IntroOfferEligibility
    get() = with(product.defaultOption) {
        if (this?.isBasePlan == true) {
            IntroOfferEligibility.INELIGIBLE
        } else if (this?.pricingPhases?.size == 2) {
            IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE
        } else {
            IntroOfferEligibility.MULTIPLE_OFFER_ELIGIBLE
        }
    }

internal val TemplateConfiguration.PackageInfo.introEligibility: IntroOfferEligibility
    get() = this.rcPackage.introEligibility
