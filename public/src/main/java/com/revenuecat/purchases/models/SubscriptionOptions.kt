package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class SubscriptionOptions(
    private val subscriptionOptions: List<SubscriptionOption>
    ) : List<SubscriptionOption> by subscriptionOptions, Parcelable {

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriptionOptions

        if (listOf(this.subscriptionOptions) != listOf(other.subscriptionOptions)) return false

        return true
    }

    override fun hashCode(): Int {
        return listOf(this.subscriptionOptions).hashCode()
    }
}
