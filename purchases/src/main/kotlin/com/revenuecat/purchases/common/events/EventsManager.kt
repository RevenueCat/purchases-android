package com.revenuecat.purchases.common.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ads.events.AdEvent
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.verboseLog
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
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the tracking, storing, and syncing of events in RevenueCat.
 *
 * @property legacyEventsFileHelper File helper for legacy paywall events.
 * @property fileHelper File helper for new backend stored events.
 * @property adFileHelper File helper for ad events tracking.
 * @property identityManager Manages user identity within the system.
 * @property eventsDispatcher Dispatches event-related operations.
 * @property postEvents Function for sending events to the backend.
 */
@Suppress("LongParameterList")
internal class EventsManager(
    private val appSessionID: UUID = Companion.appSessionID,
    private val legacyEventsFileHelper: EventsFileHelper<PaywallStoredEvent>,
    private val fileHelper: EventsFileHelper<BackendStoredEvent>,
    private val adFileHelper: EventsFileHelper<BackendStoredEvent.Ad>,
    private val identityManager: IdentityManager,
    private val eventsDispatcher: Dispatcher,
    private val postEvents: (
        EventsRequest,
        URL,
        () -> Unit,
        (error: PurchasesError, shouldMarkAsSynced: Boolean) -> Unit,
    ) -> Unit,
) {

    companion object {
        private const val FLUSH_COUNT = 50
        private const val PAYWALL_EVENTS_FILE_PATH = "RevenueCat/paywall_event_store/paywall_event_store.jsonl"
        internal const val EVENTS_FILE_PATH_NEW = "RevenueCat/event_store/event_store.jsonl"
        internal const val AD_EVENTS_FILE_PATH = "RevenueCat/event_store/ad_event_store.jsonl"
        internal val appSessionID: UUID = UUID.randomUUID()

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
        fun adEvents(fileHelper: FileHelper): EventsFileHelper<BackendStoredEvent.Ad> {
            return EventsFileHelper(
                fileHelper,
                AD_EVENTS_FILE_PATH,
                { event -> json.encodeToString(BackendStoredEvent.Ad.serializer(), event) },
                { jsonString -> json.decodeFromString(BackendStoredEvent.Ad.serializer(), jsonString) },
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

    private var flushInProgress = AtomicBoolean(false)

    private var adFlushInProgress = AtomicBoolean(false)

    @get:Synchronized
    @set:Synchronized
    private var legacyFlushTriggered = false

    /**
     * Tracks an event and stores it in the event file for future syncing.
     *
     * @param event The event to be tracked.
     */
    @OptIn(InternalRevenueCatAPI::class)
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
                else -> null
            }

            if (backendEvent != null) {
                fileHelper.appendEvent(backendEvent)
            } else {
                debugLog { "Backend event not implemented for: $event" }
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun track(event: AdEvent) {
        val backendEvent = when (event) {
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
        }

        fileHelper.appendEvent(backendEvent)
    }

    /**
     * Initiates flushing of stored events to the backend.
     */
    @Synchronized
    fun flushEvents() {
        enqueue {
            if (flushInProgress.getAndSet(true)) {
                debugLog { "Flush already in progress." }
                return@enqueue
            }

            if (!legacyFlushTriggered) {
                legacyFlushTriggered = true
                flushLegacyEvents()
            }

            val storedEventsWithNullValues = getStoredEvents()
            val storedEvents = storedEventsWithNullValues.filterNotNull()

            if (storedEvents.isEmpty()) {
                verboseLog { "No new events to sync." }
                flushInProgress.set(false)
                return@enqueue
            }

            verboseLog { "New event flush: posting ${storedEvents.size} events." }
            postEvents(
                EventsRequest(storedEvents.map { it.toBackendEvent() }),
                AppConfig.paywallEventsURL,
                {
                    verboseLog { "New event flush: success." }
                    enqueue {
                        fileHelper.clear(storedEventsWithNullValues.size)
                        flushInProgress.set(false)
                    }
                },
                { error, shouldMarkAsSynced ->
                    errorLog { "New event flush error: $error." }
                    enqueue {
                        if (shouldMarkAsSynced) {
                            fileHelper.clear(storedEventsWithNullValues.size)
                        }
                        flushInProgress.set(false)
                    }
                },
            )
        }
    }

    @Synchronized
    fun flushAdEvents() {
        enqueue {
            if (adFlushInProgress.getAndSet(true)) {
                debugLog { "Ad Flush already in progress." }
                return@enqueue
            }

            val storedEventsWithNullValues = getAdStoredEvents()
            val storedEvents = storedEventsWithNullValues.filterNotNull()

            if (storedEvents.isEmpty()) {
                verboseLog { "No new ad events to sync." }
                adFlushInProgress.set(false)
                return@enqueue
            }

            verboseLog { "New ad event flush: posting ${storedEvents.size} events." }
            postEvents(
                EventsRequest(storedEvents.map { it.toBackendEvent() }),
                AppConfig.adEventsURL,
                {
                    verboseLog { "New ad event flush: success." }
                    enqueue {
                        adFileHelper.clear(storedEventsWithNullValues.size)
                        adFlushInProgress.set(false)
                    }
                },
                { error, shouldMarkAsSynced ->
                    errorLog { "New ad event flush error: $error." }
                    enqueue {
                        if (shouldMarkAsSynced) {
                            adFileHelper.clear(storedEventsWithNullValues.size)
                        }
                        adFlushInProgress.set(false)
                    }
                },
            )
        }
    }

    /**
     * Flushes legacy paywall events to the backend.
     */
    private fun flushLegacyEvents() {
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
                AppConfig.paywallEventsURL,
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

    private fun getAdStoredEvents(): List<BackendStoredEvent.Ad?> {
        var events: List<BackendStoredEvent.Ad?> = emptyList()
        adFileHelper.readFile { sequence ->
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
        legacyEventsFileHelper.readFile { sequence ->
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
