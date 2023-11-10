package com.revenuecat.purchases.paywalls.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PaywallEventRequest(
    val events: List<PaywallBackendEvent>,
) {
    companion object {
        val json: Json = Json.Default
    }

    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
