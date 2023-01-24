package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.GooglePurchasingData

fun ProductDetails.SubscriptionOfferDetails.toSubscriptionOption(
    productId: String,
    productDetails: ProductDetails
): GoogleSubscriptionOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }

    val purchaseInfo = GooglePurchasingData.Subscription(
        productId,
        productDetails,
        subscriptionOptionId,
        offerToken
    )

    return GoogleSubscriptionOption(subscriptionOptionId, pricingPhases, offerTags, purchaseInfo)
}

private val ProductDetails.SubscriptionOfferDetails.subscriptionOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"
