package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.events.CustomerCenterBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.utils.Event
import com.revenuecat.purchases.utils.asMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
internal sealed class BackendEvent : Event {
    @Serializable
    @SerialName("customer_center")
    data class CustomerCenter(val event: CustomerCenterBackendEvent) : BackendEvent()

    @Serializable
    @SerialName("paywalls")
    data class Paywalls(val event: PaywallBackendEvent) : BackendEvent()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun PaywallEvent.toBackendEvent(appUserID: String): BackendEvent {
    return BackendEvent.Paywalls(
        PaywallBackendEvent(
            id = creationData.id.toString(),
            version = PaywallBackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
            type = type.value,
            appUserID = appUserID,
            sessionID = data.sessionIdentifier.toString(),
            offeringID = data.offeringIdentifier,
            paywallRevision = data.paywallRevision,
            timestamp = creationData.date.time,
            displayMode = data.displayMode,
            darkMode = data.darkMode,
            localeIdentifier = data.localeIdentifier,
        ),
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun BackendEvent.toEventRequestMap(): Map<String, JsonElement>? {
    val jsonElement = when (this) {
        is BackendEvent.Paywalls -> EventRequest.json.encodeToJsonElement(this.event)
        else -> null
    }
    return jsonElement?.jsonObject?.asMap()
}
