package com.revenuecat.purchases.ads.events.networking

import com.revenuecat.purchases.common.events.BackendEvent
import kotlinx.serialization.Serializable

@Serializable
internal data class AdEventsRequest(
    public val events: List<BackendEvent.Ad>,
) {
    /**
     * Generates a cache key based on the hash codes of the contained events.
     *
     * @return A list of string representations of hash codes for the events in the request.
     */
    public val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
