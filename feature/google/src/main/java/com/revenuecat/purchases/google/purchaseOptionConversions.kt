package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.GooglePurchasingData

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(
    productId: String,
    productDetails: ProductDetails
): GoogleSubscriptionOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }

    val purchaseInfo = GooglePurchasingData.Subscription(
        productId,
        productDetails,
        purchaseOptionId,
        offerToken
    )

    return GoogleSubscriptionOption(purchaseOptionId, pricingPhases, offerTags, purchaseInfo)
}

private val ProductDetails.SubscriptionOfferDetails.purchaseOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"
