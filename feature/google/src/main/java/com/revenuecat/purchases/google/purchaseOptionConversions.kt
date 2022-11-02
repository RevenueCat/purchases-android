package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.PurchaseOption

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(): PurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return PurchaseOption(pricingPhases, offerTags, offerToken)
}
