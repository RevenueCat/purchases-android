package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption

fun ProductDetails.SubscriptionOfferDetails.toSubscriptionOption(
    productId: String,
    productDetails: ProductDetails,
): GoogleSubscriptionOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return GoogleSubscriptionOption(
        productId,
        basePlanId,
        offerId,
        pricingPhases,
        offerTags,
        productDetails,
        offerToken,
    )
}

val ProductDetails.SubscriptionOfferDetails.subscriptionBillingPeriod: String?
    get() = this.pricingPhases.pricingPhaseList.lastOrNull()?.billingPeriod

val ProductDetails.SubscriptionOfferDetails.isBasePlan: Boolean
    get() = this.pricingPhases.pricingPhaseList.size == 1
