package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.paywalls.events.CustomerCenterBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.utils.Event
import kotlinx.serialization.Serializable

@Serializable
internal sealed class BackendEvent : Event {
    data class CustomerCenter(val event: CustomerCenterBackendEvent) : BackendEvent()
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
