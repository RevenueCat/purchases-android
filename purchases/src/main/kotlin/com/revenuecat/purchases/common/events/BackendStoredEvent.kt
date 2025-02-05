package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class BackendStoredEvent : Event {
    @Serializable
    @SerialName("customer_center")
    data class CustomerCenter(val event: BackendEvent.CustomerCenter) : BackendStoredEvent()

    @Serializable
    @SerialName("paywalls")
    data class Paywalls(val event: BackendEvent.Paywalls) : BackendStoredEvent()
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun BackendStoredEvent.toBackendEvent(): BackendEvent? {
    return when (this) {
        is BackendStoredEvent.Paywalls -> {
            // Convert the stored Paywalls event to the flat backend event.
            BackendEvent.Paywalls(
                id = this.event.id,
                version = this.event.version,
                type = this.event.type,
                appUserID = this.event.appUserID,
                sessionID = this.event.sessionID,
                offeringID = this.event.offeringID,
                paywallRevision = this.event.paywallRevision,
                timestamp = this.event.timestamp,
                displayMode = this.event.displayMode,
                darkMode = this.event.darkMode,
                localeIdentifier = this.event.localeIdentifier,
            )
        }
        is BackendStoredEvent.CustomerCenter -> {
            // If you have a similar conversion for customer center events, implement it here.
            // For now, returning null:
            null
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun PaywallEvent.toBackendStoredEvent(appUserID: String): BackendStoredEvent {
    return BackendStoredEvent.Paywalls(
        BackendEvent.Paywalls(
            id = creationData.id.toString(),
            version = BackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
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
