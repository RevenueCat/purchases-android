package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GooglePurchaseOption
import com.revenuecat.purchases.models.PurchaseOption

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(): GooglePurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return GooglePurchaseOption(basePlanId, offerId, pricingPhases, offerTags, offerToken)
}