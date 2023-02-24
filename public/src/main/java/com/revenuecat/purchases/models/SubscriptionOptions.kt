package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class SubscriptionOptions(
    val subscriptionOptions: List<SubscriptionOption>
    ) : MutableList<SubscriptionOption> by mutableListOf(), Parcelable {

    init {
        this.addAll(subscriptionOptions)
    }

    /**
     * Finds the first [SubscriptionOption] with a free trial [PricingPhase].
     */
    val freeTrial: SubscriptionOption?
        get() = this.firstOrNull { it.freePhase != null }

    /**
     * Finds the first [SubscriptionOption] with an intro trial [PricingPhase].
     * There can be a free trial [PricingPhase] and intro trial [PricingPhase] in the same [SubscriptionOption].
     */
    val introTrial: SubscriptionOption?
        get() = this.firstOrNull { it.introPhase != null }

    /**
     * Finds all [SubscriptionOption]s with a specific tag.
     */
    fun withTag(tag: String): List<SubscriptionOption> {
        return this.filter { it.tags.contains(tag) }
    }
}
