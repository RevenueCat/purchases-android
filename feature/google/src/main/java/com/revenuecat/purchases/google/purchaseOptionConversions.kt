package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GooglePurchaseInfo
import com.revenuecat.purchases.models.GooglePurchaseOption

fun ProductDetails.SubscriptionOfferDetails.toPurchaseOption(productId: String, productDetails: ProductDetails): GooglePurchaseOption {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.toRevenueCatPricingPhase() }

    val purchaseInfo = GooglePurchaseInfo.Subscription(
        productId,
        productDetails,
        purchaseOptionId,
        offerToken
    )

    return GooglePurchaseOption(purchaseOptionId, pricingPhases, offerTags, offerToken, purchaseInfo)
}

private val ProductDetails.SubscriptionOfferDetails.purchaseOptionId
    get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"
