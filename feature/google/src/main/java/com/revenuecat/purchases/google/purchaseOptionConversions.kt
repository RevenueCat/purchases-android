package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption

fun ProductDetails.SubscriptionOfferDetails.toSubscriptionOption(): GoogleSubscriptionOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return GoogleSubscriptionOption(subscriptionOptionId, pricingPhases, offerTags, offerToken)
}

private val ProductDetails.SubscriptionOfferDetails.subscriptionOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"
