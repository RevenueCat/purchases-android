package com.revenuecat.purchases.paywalls.events

import kotlinx.serialization.Serializable

@Serializable
internal data class PaywallEventRequest(
    val events: List<PaywallBackendEvent>,
) {
    val cacheKey: List<String>
        get() = events.map { it.hashCode().toString() }
}
