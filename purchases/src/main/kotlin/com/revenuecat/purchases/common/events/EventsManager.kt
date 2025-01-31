package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.EventsFileHelper

internal class EventsManager(
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
            val storedEventsWithNullValues = getStoredEvents()
            var storedEvents = storedEventsWithNullValues.filterNotNull()

            if (storedEvents.isEmpty()) {
                verboseLog("No events to sync.")
                flushInProgress = false
                return@enqueue
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

    private fun getStoredEvents(): List<BackendEvent?> {
        var events: List<BackendEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            events = sequence.take(flushCount).toList()
        }
        return events
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        eventsDispatcher.enqueue({ command() }, delay)
    }
}
