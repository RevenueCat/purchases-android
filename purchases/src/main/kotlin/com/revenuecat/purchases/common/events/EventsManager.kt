package com.revenuecat.purchases.common.events

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.utils.EventsFileHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal class EventsManager(
    // legacy events store for paywalls
    private val legacyEventsFileHelper: EventsFileHelper<PaywallStoredEvent>,
    private val fileHelper: EventsFileHelper<BackendEvent>,
    private val identityManager: IdentityManager,
    private val eventsDispatcher: Dispatcher,
    private val postEvents: (
        EventRequest,
        () -> Unit,
        (error: PurchasesError, shouldMarkAsSynced: Boolean) -> Unit,
    ) -> Unit,
) {

    companion object {
        private const val FLUSH_COUNT = 50
        private const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EVENTS_FILE_PATH_NEW = "RevenueCat/event_store/event_store.jsonl"

        private val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendEvent::class) {
                    subclass(BackendEvent.CustomerCenter::class, BackendEvent.CustomerCenter.serializer())
                    subclass(BackendEvent.Paywalls::class, BackendEvent.Paywalls.serializer())
                }
            }
        }

        fun backendEvents(fileHelper: FileHelper): EventsFileHelper<BackendEvent> {
            return EventsFileHelper(
                fileHelper,
                EVENTS_FILE_PATH_NEW,
                { event -> json.encodeToString(BackendEvent.serializer(), event) },
                { jsonString -> json.decodeFromString(BackendEvent.serializer(), jsonString) },
            )
        }

        fun paywalls(fileHelper: FileHelper): EventsFileHelper<PaywallStoredEvent> {
            return EventsFileHelper(
                fileHelper,
                EventsManager.PAYWALL_EVENTS_FILE_PATH,
                PaywallStoredEvent::toString,
                PaywallStoredEvent::fromString,
            )
        }
    }

    @get:Synchronized
    @set:Synchronized
    private var flushInProgress = false

    @get:Synchronized
    @set:Synchronized
    private var legacyFlushTriggered = false

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Synchronized
    fun track(event: FeatureEvent) {
        enqueue {
            debugLog("Tracking event: $event")

            val backendEvent = when {
                event.paywallEvent != null -> event.paywallEvent!!.toBackendEvent(identityManager.currentAppUserID)
                else -> null
            }

            if (backendEvent != null) {
                fileHelper.appendEvent(backendEvent)
            } else {
                debugLog("Backend event not implemented for: $event")
            }
        }
    }

    @Synchronized
    fun flushEvents() {
        enqueue {
            if (flushInProgress) {
                debugLog("Flush already in progress.")
                return@enqueue
            }
            flushInProgress = true

            if (!legacyFlushTriggered) {
                flushLegacyEvents()
                legacyFlushTriggered = true
            }

            val storedEventsWithNullValues = getStoredEvents()
            val storedEvents = storedEventsWithNullValues.filterNotNull()

            if (storedEvents.isEmpty()) {
                verboseLog("No new events to sync.")
                flushInProgress = false
                return@enqueue
            }

            verboseLog("New event flush: posting ${storedEvents.size} events.")
            postEvents(
                EventRequest(storedEvents),
                {
                    verboseLog("New event flush: success.")
                    enqueue {
                        fileHelper.clear(storedEventsWithNullValues.size)
                        flushInProgress = false
                    }
                },
                { error, shouldMarkAsSynced ->
                    errorLog("New event flush error: $error.")
                    enqueue {
                        if (shouldMarkAsSynced) {
                            fileHelper.clear(storedEventsWithNullValues.size)
                        }
                        flushInProgress = false
                    }
                },
            )
        }
    }

    private fun flushLegacyEvents() {
        enqueue {
            val storedLegacyEventsWithNullValues = getLegacyPaywallsStoredEvents()
            val storedLegacyEvents = storedLegacyEventsWithNullValues.filterNotNull()
            val storedBackendEvents = storedLegacyEvents.map { BackendEvent.Paywalls(it.toPaywallBackendEvent()) }

            if (storedLegacyEvents.isEmpty()) {
                verboseLog("No legacy events to sync. Skipping legacy flush.")
                return@enqueue
            }

            verboseLog("Legacy event flush: posting ${storedBackendEvents.size} events.")
            postEvents(
                EventRequest(storedBackendEvents),
                {
                    verboseLog("Legacy event flush: success.")
                    enqueue { legacyEventsFileHelper.clear(storedLegacyEventsWithNullValues.size) }
                },
                { error, shouldMarkAsSynced ->
                    errorLog("Legacy event flush error: $error.")
                    enqueue {
                        if (shouldMarkAsSynced) {
                            legacyEventsFileHelper.clear(storedLegacyEventsWithNullValues.size)
                        }
                    }
                },
            )
        }
    }

    private fun getStoredEvents(): List<BackendEvent?> {
        var events: List<BackendEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            events = sequence.take(FLUSH_COUNT).toList()
        }
        return events
    }

    private fun getLegacyPaywallsStoredEvents(): List<PaywallStoredEvent?> {
        var events: List<PaywallStoredEvent?> = emptyList()
        legacyEventsFileHelper.readFile { sequence ->
            events = sequence.take(FLUSH_COUNT).toList()
        }
        return events
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        eventsDispatcher.enqueue({ command() }, delay)
    }
}
