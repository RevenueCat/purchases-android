@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.events

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.DebugEvent
import com.revenuecat.purchases.DebugEventListener
import com.revenuecat.purchases.DebugEventName
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ads.events.AdEvent
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.customercenter.events.CustomerCenterImpressionEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterSurveyOptionChosenEvent
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.utils.EventsFileHelper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the tracking, storing, and syncing of events in RevenueCat.
 *
 * @property legacyEventsFileHelper File helper for legacy paywall events.
 * @property fileHelper File helper for new backend stored events.
 * @property identityManager Manages user identity within the system.
 * @property eventsDispatcher Dispatches event-related operations.
 * @property postEvents Function for sending events to the backend.
 */
@Suppress("LongParameterList")
internal class EventsManager(
    private val appSessionID: UUID = Companion.appSessionID,
    private val legacyEventsFileHelper: EventsFileHelper<PaywallStoredEvent>?,
    private val fileHelper: EventsFileHelper<BackendStoredEvent>,
    private val identityManager: IdentityManager,
    private val eventsDispatcher: Dispatcher,
    private val postEvents: (
        EventsRequest,
        Delay,
        () -> Unit,
        (error: PurchasesError, shouldMarkAsSynced: Boolean) -> Unit,
    ) -> Unit,
) {

    companion object {
        private const val FLUSH_COUNT = 50
        private const val MAX_FLUSH_BATCHES = 10
        private const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
        internal const val EVENTS_FILE_PATH_NEW = "RevenueCat/event_store/event_store.jsonl"
        internal const val AD_EVENTS_FILE_PATH = "RevenueCat/event_store/ad_event_store.jsonl"
        internal val appSessionID: UUID = UUID.randomUUID()

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val FILE_SIZE_LIMIT_KB = 2048.0

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val EVENTS_TO_CLEAR_ON_LIMIT = 50

        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            serializersModule = SerializersModule {
                polymorphic(BackendStoredEvent::class) {
                    subclass(BackendStoredEvent.CustomerCenter::class, BackendStoredEvent.CustomerCenter.serializer())
                    subclass(BackendStoredEvent.Paywalls::class, BackendStoredEvent.Paywalls.serializer())
                    subclass(BackendStoredEvent.Ad::class, BackendStoredEvent.Ad.serializer())
                }
            }
            explicitNulls = false
        }

        /**
         * Creates an `EventsFileHelper` for handling backend events.
         *
         * @param fileHelper The file helper used for event storage.
         * @return An `EventsFileHelper` for `BackendStoredEvent`.
         */
        fun backendEvents(fileHelper: FileHelper): EventsFileHelper<BackendStoredEvent> {
            return EventsFileHelper(
                fileHelper,
                EVENTS_FILE_PATH_NEW,
                { event -> json.encodeToString(BackendStoredEvent.serializer(), event) },
                { jsonString -> json.decodeFromString(BackendStoredEvent.serializer(), jsonString) },
            )
        }

        /**
         * Creates an `EventsFileHelper` for handling ad events.
         *
         * @param fileHelper The file helper used for event storage.
         * @return An `EventsFileHelper` for `BackendStoredEvent.Ad`.
         */
        fun adEvents(fileHelper: FileHelper): EventsFileHelper<BackendStoredEvent> {
            return EventsFileHelper(
                fileHelper,
                AD_EVENTS_FILE_PATH,
                { event -> json.encodeToString(BackendStoredEvent.serializer(), event) },
                { jsonString -> json.decodeFromString(BackendStoredEvent.serializer(), jsonString) },
            )
        }

        /**
         * Creates an `EventsFileHelper` for handling paywall events.
         *
         * @param fileHelper The file helper used for event storage.
         * @return An `EventsFileHelper` for `PaywallStoredEvent`.
         */
        fun paywalls(fileHelper: FileHelper): EventsFileHelper<PaywallStoredEvent> {
            return EventsFileHelper(
                fileHelper,
                PAYWALL_EVENTS_FILE_PATH,
                PaywallStoredEvent::toString,
                PaywallStoredEvent::fromString,
            )
        }
    }

    @get:Synchronized
    @set:Synchronized
    var debugEventListener: DebugEventListener? = null
        set(value) {
            field = value
            val callback: ((DebugEvent) -> Unit)? = value?.let { listener ->
                {
                        event: DebugEvent ->
                    listener.onDebugEventReceived(event)
                }
            }
            fileHelper.debugEventCallback = callback
        }

    private var flushInProgress = AtomicBoolean(false)

    @get:Synchronized
    @set:Synchronized
    private var legacyFlushTriggered = false

    /**
     * Checks if the event file size exceeds the limit and clears oldest events if needed.
     */
    private fun checkFileSizeAndClearIfNeeded() {
        val currentFileSizeKB = fileHelper.fileSizeInKB()
        if (currentFileSizeKB >= FILE_SIZE_LIMIT_KB) {
            warnLog { "Event store size limit reached. Clearing oldest events to free up space." }
            fileHelper.clear(EVENTS_TO_CLEAR_ON_LIMIT)
            debugEventListener?.onDebugEventReceived(
                DebugEvent(name = DebugEventName.FILE_SIZE_LIMIT_REACHED),
            )
        }
    }

    /**
     * Tracks an event and stores it in the event file for future syncing.
     *
     * @param event The event to be tracked.
     */
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Synchronized
    fun track(event: FeatureEvent) {
        enqueue {
            debugLog { "Tracking event: $event" }

            val backendEvent = when (event) {
                is PaywallEvent -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                )
                is CustomerCenterImpressionEvent -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is CustomerCenterSurveyOptionChosenEvent -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is AdEvent.Displayed -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is AdEvent.Open -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is AdEvent.Revenue -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is AdEvent.Loaded -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                is AdEvent.FailedToLoad -> event.toBackendStoredEvent(
                    identityManager.currentAppUserID,
                    appSessionID.toString(),
                )
                else -> null
            }

            if (backendEvent != null) {
                checkFileSizeAndClearIfNeeded()
                fileHelper.appendEvent(backendEvent)
            } else {
                debugLog { "Backend event not implemented for: $event" }
            }
        }
    }

    /**
     * Initiates flushing of stored events to the backend.
     */
    @Synchronized
    fun flushEvents(delay: Delay = Delay.DEFAULT) {
        enqueue {
            if (flushInProgress.getAndSet(true)) {
                debugLog { "Flush already in progress." }
                return@enqueue
            }
            debugEventListener?.onDebugEventReceived(
                DebugEvent(name = DebugEventName.FLUSH_STARTED),
            )

            flushNextBatch(batchNumber = 1, delay = delay)

            if (!legacyFlushTriggered) {
                legacyFlushTriggered = true
                flushLegacyEvents()
            }
        }
    }

    /**
     * Flushes the next batch of events.
     *
     * @param batchNumber The current batch number being flushed.
     */
    private fun flushNextBatch(batchNumber: Int, delay: Delay) {
        if (batchNumber > MAX_FLUSH_BATCHES) {
            verboseLog { "Reached maximum number of flush batches ($MAX_FLUSH_BATCHES). Stopping flush." }
            flushInProgress.set(false)
            return
        }

        val storedEventsWithNullValues = getStoredEvents()
        val storedEvents = storedEventsWithNullValues.filterNotNull()

        if (storedEvents.isEmpty()) {
            verboseLog { "No new events to sync." }
            flushInProgress.set(false)
            return
        }

        verboseLog { "New event flush (batch $batchNumber): posting ${storedEvents.size} events." }
        postEvents(
            EventsRequest(storedEvents.map { it.toBackendEvent() }),
            delay,
            {
                verboseLog { "New event flush (batch $batchNumber): success." }
                enqueue {
                    fileHelper.clear(storedEventsWithNullValues.size)
                    // Continue flushing next batch
                    flushNextBatch(batchNumber + 1, delay)
                }
            },
            { error, shouldMarkAsSynced ->
                errorLog { "New event flush (batch $batchNumber) error: $error." }
                debugEventListener?.onDebugEventReceived(
                    DebugEvent(
                        name = DebugEventName.FLUSH_ERROR,
                        properties = buildMap {
                            put("errorCode", error.code.name)
                            error.underlyingErrorMessage?.let {
                                put("underlyingErrorMessage", it.take(80))
                            }
                        },
                    ),
                )
                enqueue {
                    if (shouldMarkAsSynced) {
                        fileHelper.clear(storedEventsWithNullValues.size)
                    }
                    // Stop flushing on error
                    flushInProgress.set(false)
                }
            },
        )
    }

    /**
     * Flushes legacy paywall events to the backend.
     */
    private fun flushLegacyEvents() {
        val legacyEventsFileHelper = this.legacyEventsFileHelper
            ?: return // No legacy events file helper provided; nothing to flush.
        enqueue {
            val storedLegacyEventsWithNullValues = getLegacyPaywallsStoredEvents()
            val storedLegacyEvents = storedLegacyEventsWithNullValues.filterNotNull()
            val storedBackendEvents = storedLegacyEvents.map { BackendStoredEvent.Paywalls(it.toBackendEvent()) }

            if (storedLegacyEvents.isEmpty()) {
                verboseLog { "No legacy events to sync. Skipping legacy flush." }
                return@enqueue
            }

            verboseLog { "Legacy event flush: posting ${storedBackendEvents.size} events." }
            postEvents(
                EventsRequest(storedBackendEvents.map { it.toBackendEvent() }),
                Delay.LONG,
                {
                    verboseLog { "Legacy event flush: success." }
                    enqueue { legacyEventsFileHelper.clear(storedLegacyEventsWithNullValues.size) }
                },
                { error, shouldMarkAsSynced ->
                    errorLog { "Legacy event flush error: $error." }
                    enqueue {
                        if (shouldMarkAsSynced) {
                            legacyEventsFileHelper.clear(storedLegacyEventsWithNullValues.size)
                        }
                    }
                },
            )
        }
    }

    /**
     * Retrieves stored backend events from the file system.
     *
     * @return A list of stored backend events, some of which may be null.
     */
    private fun getStoredEvents(): List<BackendStoredEvent?> {
        var events: List<BackendStoredEvent?> = emptyList()
        fileHelper.readFile { sequence ->
            events = sequence.take(FLUSH_COUNT).toList()
        }
        return events
    }

    /**
     * Retrieves stored legacy paywall events from the file system.
     *
     * @return A list of stored paywall events, some of which may be null.
     */
    private fun getLegacyPaywallsStoredEvents(): List<PaywallStoredEvent?> {
        var events: List<PaywallStoredEvent?> = emptyList()
        legacyEventsFileHelper?.readFile { sequence ->
            events = sequence.take(FLUSH_COUNT).toList()
        }
        return events
    }

    /**
     * Enqueues a task for execution.
     *
     * @param delay The delay before execution.
     * @param command The task to execute.
     */
    private fun enqueue(delay: Delay = Delay.NONE, command: () -> Unit) {
        eventsDispatcher.enqueue({ command() }, delay)
    }
}
