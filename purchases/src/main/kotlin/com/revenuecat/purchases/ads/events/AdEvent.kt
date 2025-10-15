package com.revenuecat.purchases.ads.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
enum class AdEventType(val type: String) {
    DISPLAYED("displayed"),
    OPENED("opened"),
    REVENUE("revenue"),
}

@InternalRevenueCatAPI
@Serializable
sealed interface AdEvent {
    @SerialName("event_id")
    val eventId: String

    @SerialName("event_version")
    val eventVersion: Int
        get() = 1

    val type: AdEventType
    val timestamp: Long
}
