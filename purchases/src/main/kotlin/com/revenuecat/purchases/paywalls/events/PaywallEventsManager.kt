package com.revenuecat.purchases.paywalls.events

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.EventsFileHelper

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RequiresApi(Build.VERSION_CODES.N)
internal class PaywallEventsManager(
    private val fileHelper: EventsFileHelper<PaywallStoredEvent>,
    private val identityManager: IdentityManager,
    private val paywallEventsDispatcher: Dispatcher,
    private val backend: Backend,
) {
    companion object {
        const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
        private const val FLUSH_COUNT = 50
    }

    @get:Synchronized
    @set:Synchronized
    private var flushInProgress = false

    @Synchronized
    fun track(event: PaywallEvent) {
        enqueue {
            debugLog("Tracking paywall event: $event")
            fileHelper.appendEvent(PaywallStoredEvent(event, identityManager.currentAppUserID))
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
            val eventsToSyncWithNullValues = getEventsToSync()
            val eventsToSync = eventsToSyncWithNullValues.filterNotNull()
            val eventsToSyncSize = eventsToSync.size
            if (eventsToSync.isEmpty()) {
                verboseLog("No paywall events to sync.")
                flushInProgress = false
                return@enqueue
            }
            verboseLog("Paywall event flush: posting $eventsToSyncSize events.")
            backend.postPaywallEvents(
                paywallEventRequest = PaywallEventRequest(eventsToSync.map { it.toPaywallBackendEvent() }),
                onSuccessHandler = {
                    verboseLog("Paywall event flush: success.")
                    enqueue {
                        fileHelper.clear(eventsToSyncWithNullValues.size)
                        flushInProgress = false
                    }
                },
                onErrorHandler = { error, shouldMarkAsSynced ->
                    errorLog("Paywall event flush error: $error.")
                    enqueue {
                        if (shouldMarkAsSynced) {
                            fileHelper.clear(eventsToSyncWithNullValues.size)
                        }
                        flushInProgress = false
                    }
                },
            )
        }
    }

    private fun getEventsToSync(): List<PaywallStoredEvent?> {
        var eventsToSync: List<PaywallStoredEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            eventsToSync = sequence.take(FLUSH_COUNT).toList()
        }
        return eventsToSync
    }

    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        paywallEventsDispatcher.enqueue({
            command()
        }, delay)
    }
}
