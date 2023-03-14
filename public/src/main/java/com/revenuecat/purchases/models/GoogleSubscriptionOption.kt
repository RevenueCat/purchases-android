package com.revenuecat.purchases.models

/**
 * Defines an option for purchasing a Google subscription
 */
data class GoogleSubscriptionOption(
    override val id: String,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,
    override val purchasingData: PurchasingData
) : SubscriptionOption
