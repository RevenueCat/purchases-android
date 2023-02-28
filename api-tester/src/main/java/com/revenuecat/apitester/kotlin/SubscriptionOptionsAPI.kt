package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.SubscriptionOptions

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class SubscriptionOptionsAPI {
    fun checkSubscriptionOptions() {
        val subscriptionOptions = SubscriptionOptions(emptyList())

        val freeTrial = subscriptionOptions.freeTrial
        val introTrial = subscriptionOptions.introTrial
        val tagOptions = subscriptionOptions.withTag("pick-this-one")

        subscriptionOptions.forEach {
            val optionId = it.id
        }
    }
}
