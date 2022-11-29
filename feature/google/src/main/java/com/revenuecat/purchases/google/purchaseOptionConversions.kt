package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GooglePurchaseOption

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(): GooglePurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }
    return GooglePurchaseOption(purchaseOptionId, pricingPhases, offerTags, offerToken)
}

private val ProductDetails.SubscriptionOfferDetails.purchaseOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"

