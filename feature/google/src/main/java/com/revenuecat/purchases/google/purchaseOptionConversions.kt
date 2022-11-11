package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GooglePurchaseOption

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(): GooglePurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return GooglePurchaseOption(offerId ?: basePlanId, pricingPhases, offerTags, offerToken)
}
