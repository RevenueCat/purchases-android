package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class SubscriptionOptionAPI {
    fun checkSubscriptionOption(subscriptionOption: SubscriptionOption) {
        val phases: List<PricingPhase> = subscriptionOption.pricingPhases
        val tags: List<String> = subscriptionOption.tags
        val isBasePlan: Boolean = subscriptionOption.isBasePlan
        val presentedOfferingId: String? = subscriptionOption.presentedOfferingIdentifier
        val isPrepaid: Boolean = subscriptionOption.isPrepaid
    }
}
