package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
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
    private val flushCount: Int = 50,
) {

    companion object {
        const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
        const val EVENTS_FILE_PATH_NEW = "RevenueCat/event_store/event_store.jsonl"

        internal val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendEvent::class) {
                    subclass(BackendEvent.CustomerCenter::class, BackendEvent.CustomerCenter.serializer())
                    subclass(BackendEvent.Paywalls::class, BackendEvent.Paywalls.serializer())
                }
            }
        }
    }

    @get:Synchronized
    @set:Synchronized
    private var flushInProgress = false

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
            flushLegacyEvents {
                val storedEventsWithNullValues = getStoredEvents()
                val storedEvents = storedEventsWithNullValues.filterNotNull()

                if (storedEvents.isEmpty()) {
                    verboseLog("No events to sync.")
                    flushInProgress = false
                    return@flushLegacyEvents
                }

                verboseLog("Event flush: posting ${storedEvents.size} events.")
                postEvents(
                    EventRequest(storedEvents),
                    {
                        verboseLog("Event flush: success.")
                        enqueue {
                            fileHelper.clear(storedEventsWithNullValues.size)
                            flushInProgress = false
                        }
                    },
                    { error, shouldMarkAsSynced ->
                        errorLog("Event flush error: $error.")
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
    }

    private fun flushLegacyEvents(onComplete: () -> Unit) {
        enqueue {
            val storedLegacyEvents = getLegacyStoredEvents()

            if (storedLegacyEvents.isEmpty()) {
                verboseLog("No legacy events to sync.")
                onComplete() // ✅ Proceed with new events
                return@enqueue
            }

            verboseLog("Legacy event flush: posting ${storedLegacyEvents.size} events.")

            postEvents(
                EventRequest(storedLegacyEvents),
                {
                    verboseLog("Legacy event flush: success.")
                    enqueue {
                        legacyEventsFileHelper.clear(flushCount)
                        onComplete()
                    }
                },
                { error, shouldMarkAsSynced ->
                    errorLog("Legacy event flush error: $error.")
                    enqueue {
                        if (shouldMarkAsSynced) {
                            legacyEventsFileHelper.clear(flushCount)
                        }
                        onComplete()
                    }
                },
            )
        }
    }

    private fun getStoredEvents(): List<BackendEvent?> {
        var events: List<BackendEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            events = sequence.take(flushCount).toList()
        }
        return events
    }

    private fun getLegacyStoredEvents(): List<BackendEvent> {
        var events: List<PaywallStoredEvent?> = emptyList()
        legacyEventsFileHelper.readFile { sequence ->
            events = sequence.take(flushCount).toList()
        }
        return events
            .filterNotNull()
            .map { BackendEvent.Paywalls(it.toPaywallBackendEvent()) }
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        eventsDispatcher.enqueue({ command() }, delay)
    }
}
