package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.Event
import com.revenuecat.purchases.utils.EventsFileHelper

internal abstract class EventsManager<FeatureEvent, StoredEvent : Event, BackendEvent, EventRequest>(
    private val fileHelper: EventsFileHelper<StoredEvent>,
    private val identityManager: IdentityManager,
    private val eventsDispatcher: Dispatcher,
    private val postEvents: (
        EventRequest,
        () -> Unit,
        (error: PurchasesError, shouldMarkAsSynced: Boolean) -> Unit,
    ) -> Unit,
    private val flushCount: Int = 50,
) {

    @get:Synchronized
    @set:Synchronized
    private var flushInProgress = false

    @Synchronized
    fun track(event: FeatureEvent) {
        enqueue {
            debugLog("Tracking event: $event")
            fileHelper.appendEvent(createStoredEvent(event, identityManager.currentAppUserID))
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
            val storedEventsWithNullValues = getStoredEvents()
            var storedEvents = storedEventsWithNullValues.filterNotNull()

            if (storedEvents.isEmpty()) {
                verboseLog("No events to sync.")
                flushInProgress = false
                return@enqueue
            }

            verboseLog("Event flush: posting ${storedEvents.size} events.")
            postEvents(
                createRequest(storedEvents),
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

    private fun getStoredEvents(): List<StoredEvent?> {
        var events: List<StoredEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            events = sequence.take(flushCount).toList()
        }
        return events
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        eventsDispatcher.enqueue({ command() }, delay)
    }

    protected abstract fun createRequest(events: List<StoredEvent>): EventRequest
    protected abstract fun createStoredEvent(event: FeatureEvent, currentAppUserID: String): StoredEvent
    protected abstract fun fromStoredEvent(event: StoredEvent): FeatureEvent
    protected abstract fun toBackendEvent(event: StoredEvent): BackendEvent
}
