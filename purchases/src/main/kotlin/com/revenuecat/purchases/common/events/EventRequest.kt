package com.revenuecat.purchases.common.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
public data class EventRequest internal constructor(
    internal val events: List<BackendEvent>,
) {

    companion object {
        val json: Json = Json.Default
    }

    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
