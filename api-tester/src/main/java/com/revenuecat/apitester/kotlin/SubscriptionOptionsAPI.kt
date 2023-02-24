package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class SubscriptionOptionsAPI {
    fun checkSubscriptionOptions(subscriptionOptions: SubscriptionOptions) {
        val freeTrial = subscriptionOptions.freeTrial
        val introTrial = subscriptionOptions.introTrial
        val tagOptions = subscriptionOptions.withTag("pick-this-one")

        subscriptionOptions.forEach {
            val optionId = it.id
        }
    }
}
