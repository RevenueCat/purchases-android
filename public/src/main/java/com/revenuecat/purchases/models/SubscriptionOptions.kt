package com.revenuecat.purchases.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubscriptionOptions(
    /**
     * List of [SubscriptionOption].
     */
    val all: List<SubscriptionOption>
) : Parcelable {

    /**
     * Finds the first [SubscriptionOption] free trial.
     */
    val freeTrial: SubscriptionOption?
        get() = all.firstOrNull { it.freePhase != null }

    /**
     * Finds the first [SubscriptionOption] with an intro trial.
     */
    val introTrial: SubscriptionOption?
        get() = all.firstOrNull { it.introPhase != null }

    /**
     * Filters
     */
    fun byTag(tag: String): List<SubscriptionOption> {
        return all.filter { it.tags.contains(tag) }
    }

    /**
     * Retrieves an specific [SubscriptionOption] by index. It's equivalent to
     * accessing the `all` map by index.
     */
    operator fun get(i: Int) = all[i]
}
