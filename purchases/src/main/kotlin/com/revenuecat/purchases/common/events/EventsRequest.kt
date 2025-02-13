package com.revenuecat.purchases.common.events

import kotlinx.serialization.Serializable

/**
 * A request object for handling events.
 *
 * @property events A list of [BackendEvent] objects representing the events in the request.
 */
@Serializable
data class EventsRequest internal constructor(
    internal val events: List<BackendEvent>,
) {
    /**
     * Generates a cache key based on the hash codes of the contained events.
     *
     * @return A list of string representations of hash codes for the events in the request.
     */
    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
