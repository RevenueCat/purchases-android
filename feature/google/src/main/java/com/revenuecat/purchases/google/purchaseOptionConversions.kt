package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(
    productId: String,
    productDetails: ProductDetails
): GooglePurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }

    val purchaseInfo = GooglePurchasingData.Subscription(
        productId,
        productDetails,
        purchaseOptionId,
        offerToken
    )

    return GooglePurchaseOption(purchaseOptionId, pricingPhases, offerTags, purchaseInfo)
}

private val ProductDetails.SubscriptionOfferDetails.purchaseOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"
