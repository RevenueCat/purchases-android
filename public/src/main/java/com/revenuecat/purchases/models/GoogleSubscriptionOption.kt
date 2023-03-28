package com.revenuecat.purchases.models

/**
 * Defines an option for purchasing a Google subscription
 */
data class GoogleSubscriptionOption(
    override val id: String,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,
    override val purchasingData: PurchasingData
) : SubscriptionOption {
    override val platformProductId: PlatformProductId
        get() = GooglePlatformProductId(
            purchasingData.productId,
            id.split(":").firstOrNull(), // TODO: fix, this is bad
            id.split(":").getOrNull(1) // TODO: fix, this is bad
        )
}
