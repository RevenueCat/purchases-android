package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.PayloadFileStore
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingsCache(
    private val deviceCache: DeviceCache,
    private val responseStore: PayloadFileStore,
    private val ioDispatcher: Dispatcher,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        dateProvider = dateProvider,
    ),
    private val localeProvider: LocaleProvider,
) {

    private class ResponseWriterState {
        val lock = ReentrantLock()
        private val currentGeneration = AtomicLong()
        private var pendingResponse: String? = null

        fun register(): Long = currentGeneration.incrementAndGet()

        fun isCurrent(generation: Long): Boolean = currentGeneration.get() == generation

        @Synchronized
        fun remember(response: String, generation: Long) {
            if (isCurrent(generation)) pendingResponse = response
        }

        @Synchronized
        fun pending(): String? = pendingResponse

        @Synchronized
        fun clear(expectedResponse: String? = null) {
            if (expectedResponse == null || pendingResponse === expectedResponse) pendingResponse = null
        }
    }

    private companion object {
        // Purchases.configure() builds the replacement before closing the previous instance. Coordinate writers
        // process-wide so an old instance cannot commit after, or write the same temp file alongside, its replacement.
        val responseWriterStates = ConcurrentHashMap<String, ResponseWriterState>()

        fun responseWriterStateFor(key: String): ResponseWriterState {
            responseWriterStates[key]?.let { return it }
            val newState = ResponseWriterState()
            return responseWriterStates.putIfAbsent(key, newState) ?: newState
        }
    }

    private var cachedLanguageTags: String? = null

    // Bumped by clearCache() on identity changes, so a fetch started for the previous user can't cache over it.
    @get:Synchronized
    var epoch: Int = 0
        private set

    private var pendingResponse: String? = null

    private val responseWriterState = responseWriterStateFor(responseKey)
    private val responseWriterGeneration = responseWriterState.register()

    @Synchronized
    fun migrateLegacyResponse() {
        val legacyResponse = deviceCache.takeLegacyOfferingsResponse()
        if (legacyResponse != null) responseWriterState.remember(legacyResponse, responseWriterGeneration)
        pendingResponse = legacyResponse ?: responseWriterState.pending() ?: return
        ioDispatcher.enqueue({ persistPendingResponse() })
    }

    @Synchronized
    fun clearCache() {
        epoch++
        offeringsCachedObject.clearCache()
        pendingResponse = null
        responseWriterState.clear()
        deviceCache.clearOfferingsResponseChecksum()
        responseStore.delete(responseKey)
        cachedLanguageTags = null
    }

    /**
     * [responsePayload] is null when [offerings] was parsed from the disk cache, whose response is already
     * on disk: re-writing it would rewrite a multi-MB file on every failed fetch.
     * Nothing is cached when [requestEpoch] predates the last [clearCache].
     */
    @Synchronized
    fun cacheOfferings(offerings: Offerings, responsePayload: String?, requestEpoch: Int = epoch) {
        if (requestEpoch != epoch) {
            debugLog { OfferingStrings.DROPPING_OFFERINGS_FROM_PREVIOUS_USER }
            return
        }
        offeringsCachedObject.cacheInstance(offerings)
        if (responsePayload != null) {
            pendingResponse = responsePayload
            responseWriterState.remember(responsePayload, responseWriterGeneration)
            ioDispatcher.enqueue({ persistPendingResponse() })
        }
        offeringsCachedObject.updateCacheTimestamp(dateProvider.now)
        cachedLanguageTags = String(localeProvider.currentLocalesLanguageTags.toCharArray())
    }

    // region Offerings cache

    val cachedOfferings: Offerings?
        @Synchronized
        get() = offeringsCachedObject.cachedInstance

    @Synchronized
    fun isOfferingsCacheStale(appInBackground: Boolean): Boolean =
        // Time-based staleness, or
        offeringsCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider) ||
            // Locale-based staleness
            cachedLanguageTags != localeProvider.currentLocalesLanguageTags

    @Synchronized
    fun clearInMemoryOfferingsCache() {
        offeringsCachedObject.clearCache()
        cachedLanguageTags = null
    }

    @Synchronized
    fun forceCacheStale() {
        offeringsCachedObject.clearCacheTimestamp()
        cachedLanguageTags = null
    }

    // endregion Offerings cache

    private val responseKey: String
        get() = deviceCache.offeringsResponseFileKey

    private fun persistPendingResponse() {
        val (responsePayload, requestEpoch) = synchronized(this) {
            pendingResponse?.let { it to epoch }
        } ?: return
        fun discardPendingResponse() {
            synchronized(this) {
                if (pendingResponse === responsePayload) pendingResponse = null
            }
        }
        responseWriterState.lock.withLock {
            if (!responseWriterState.isCurrent(responseWriterGeneration)) {
                discardPendingResponse()
            } else {
                val persisted = isAlreadyStored(responsePayload) ||
                    responseStore.write(responseKey, responsePayload)?.also { checksum ->
                        if (!responseWriterState.isCurrent(responseWriterGeneration)) return@also
                        synchronized(this) {
                            if (requestEpoch == epoch) {
                                deviceCache.cacheOfferingsResponseChecksum(checksum)
                            } else {
                                responseStore.delete(responseKey)
                            }
                        }
                    } != null
                if (!responseWriterState.isCurrent(responseWriterGeneration)) {
                    discardPendingResponse()
                } else if (persisted && requestEpoch == epoch) {
                    responseWriterState.clear(responsePayload)
                    discardPendingResponse()
                }
            }
        }
    }

    // A 304 from the ETag cache hands back the same payload on every refresh.
    private fun isAlreadyStored(responsePayload: String): Boolean {
        val storedChecksum = deviceCache.getOfferingsResponseChecksum() ?: return false
        return responseStore.contains(responseKey) && responseStore.checksumOf(responsePayload) == storedChecksum
    }

    // region Offerings response cache

    val cachedOfferingsResponse: JSONObject?
        get() = synchronized(this) { pendingResponse }?.let(::parseOrNull) ?: readStoredResponse()

    private fun readStoredResponse(): JSONObject? {
        val checksum = deviceCache.getOfferingsResponseChecksum() ?: return null
        return responseStore.read(responseKey, checksum)
            ?.let(::parseOrNull)
            .also { if (it == null) forgetUnreadableResponse(checksum) }
    }

    @Synchronized
    private fun forgetUnreadableResponse(checksum: Long) {
        if (deviceCache.getOfferingsResponseChecksum() == checksum) {
            deviceCache.clearOfferingsResponseChecksum()
        }
    }

    // endregion Offerings response cache
}

@Suppress("SwallowedException")
private fun parseOrNull(response: String): JSONObject? {
    return try {
        JSONObject(response)
    } catch (exception: JSONException) {
        null
    }
}
