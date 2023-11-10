package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.Serializable

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Serializable
internal data class PaywallStoredEvent(
    val event: PaywallEvent,
    val userID: String,
) {
    fun toPaywallBackendEvent(): PaywallBackendEvent {
        return PaywallBackendEvent(
            id = event.creationData.id.toString(),
            version = PaywallBackendEvent.PAYWALL_EVENT_SCHEMA_VERSION,
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
}
