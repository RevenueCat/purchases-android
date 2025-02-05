package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stored backend events to be flushed together by EventsManager.
 */
@Serializable
internal sealed class BackendStoredEvent : Event {

    /**
     * Represents a stored event related to the Customer Center.
     *
     * @property event The `BackendEvent.CustomerCenter` event that is being stored.
     */
    @Serializable
    @SerialName("customer_center")
    data class CustomerCenter(val event: BackendEvent.CustomerCenter) : BackendStoredEvent()

    /**
     * Represents a stored event related to Paywalls.
     *
     * @property event The `BackendEvent.Paywalls` event that is being stored.
     */
    @Serializable
    @SerialName("paywalls")
    data class Paywalls(val event: BackendEvent.Paywalls) : BackendStoredEvent()
}

/**
 * Converts a `BackendStoredEvent` into a `BackendEvent`.
 *
 * @receiver The stored backend event to be converted.
 * @return A `BackendEvent` instance or `null` if conversion is not possible.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun BackendStoredEvent.toBackendEvent(): BackendEvent? {
    return when (this) {
        is BackendStoredEvent.Paywalls -> { this.event }
        is BackendStoredEvent.CustomerCenter -> {
            // For now, returning null:
            null
        }
    }
}

/**
 * Converts a `PaywallEvent` into a `BackendStoredEvent.Paywalls` instance.
 *
 * @receiver The `PaywallEvent` to be converted.
 * @param appUserID The user ID associated with the event.
 * @return A `BackendStoredEvent.Paywalls` containing a `BackendEvent.Paywalls`.
 */
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
