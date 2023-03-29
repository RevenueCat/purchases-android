package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails

/**
 * Defines an option for purchasing a Google subscription
 */
data class GoogleSubscriptionOption(
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,
    val productDetails: ProductDetails,
    val offerToken: String,
    override val platformProductId: GooglePlatformProductId
) : SubscriptionOption {
    override val id: String
        get() = platformProductId.toId()

    override val purchasingData: PurchasingData
        get() = GooglePurchasingData.Subscription(
            platformProductId.productId,
            id,
            productDetails,
            offerToken
        )
}

data class GooglePlatformProductId(
    override val productId: String,
    val basePlanId: String? = null,
    val offerId: String? = null
) : PlatformProductId(productId) {
    override val asMap: Map<String, String?>
        get() = mapOf(
            "product_id" to productId,
            "base_plan_id" to basePlanId,
            "offer_id" to offerId
        )
}
