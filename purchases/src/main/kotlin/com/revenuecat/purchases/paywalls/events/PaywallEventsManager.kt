package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.events.EventRequest
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.EventsFileHelper

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class PaywallEventsManager(
    fileHelper: EventsFileHelper<PaywallStoredEvent>,
    identityManager: IdentityManager,
    eventsDispatcher: Dispatcher,
    flushCount: Int = PaywallEventsManager.FLUSH_COUNT,
    postPaywallEvents: (EventRequest, () -> Unit, (PurchasesError, Boolean) -> Unit) -> Unit,
) : EventsManager<PaywallEvent, PaywallStoredEvent, PaywallBackendEvent>(
    fileHelper,
    identityManager,
    eventsDispatcher,
    postPaywallEvents,
    flushCount,
) {
    companion object {
        const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
        private const val FLUSH_COUNT = 50
    }

    override fun createRequest(events: List<PaywallStoredEvent>): EventRequest {
        val backendEvents = events.map { toBackendEvent(it) }
        return EventRequest(backendEvents)
    }

    override fun createStoredEvent(event: PaywallEvent, currentAppUserID: String): PaywallStoredEvent {
        return PaywallStoredEvent(
            event,
            userID = currentAppUserID,
        )
    }

    override fun fromStoredEvent(event: PaywallStoredEvent): PaywallEvent {
        return event.event
    }

    override fun toBackendEvent(event: PaywallStoredEvent): PaywallBackendEvent {
        return event.toPaywallBackendEvent()
    }
}
