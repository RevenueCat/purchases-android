package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Serializable
internal data class PaywallStoredEvent(
    val event: PaywallEvent,
    val userID: String,
) : Event {
    companion object {
        val json = Json.Default
        fun fromString(string: String): PaywallStoredEvent {
            return json.decodeFromString(string)
        }
    }

    fun toBackendEvent(): BackendEvent.Paywalls {
        return BackendEvent.Paywalls(
            id = event.creationData.id.toString(),
            version = BackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
            type = event.type.value,
            appUserID = userID,
            sessionID = event.data.sessionIdentifier.toString(),
            offeringID = event.data.offeringIdentifier,
            paywallRevision = event.data.paywallRevision,
            timestamp = event.creationData.date.time,
            displayMode = event.data.displayMode,
            darkMode = event.data.darkMode,
            localeIdentifier = event.data.localeIdentifier,
        )
    }

    override fun toString(): String {
        return json.encodeToString(this)
    }
}
