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

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingsCache(
    private val deviceCache: DeviceCache,
    private val responseStore: PayloadFileStore,
    private val dispatcher: Dispatcher,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        dateProvider = dateProvider,
    ),
    private val localeProvider: LocaleProvider,
) {
    private var cachedLanguageTags: String? = null

    // Bumped by clearCache() on identity changes, so a fetch started for the previous user can't cache over it.
    @get:Synchronized
    var epoch: Int = 0
        private set

    private var pendingResponse: String? = null

    init {
        deviceCache.removeLegacyOfferingsResponse()
    }

    @Synchronized
    fun clearCache() {
        epoch++
        offeringsCachedObject.clearCache()
        pendingResponse = null
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
        responsePayload?.let(::persistInBackground)
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

    private fun persistInBackground(responsePayload: String) {
        pendingResponse = responsePayload
        dispatcher.enqueue({ persistPendingResponse() })
    }

    private fun persistPendingResponse() {
        val (responsePayload, requestEpoch) = synchronized(this) {
            val payload = pendingResponse ?: return
            pendingResponse = null
            payload to epoch
        }
        val checksum = responsePayload
            .takeUnless { isAlreadyStored(it) }
            ?.let { responseStore.write(responseKey, it) }
            ?: return
        synchronized(this) {
            if (requestEpoch == epoch) {
                deviceCache.cacheOfferingsResponseChecksum(checksum)
            } else {
                responseStore.delete(responseKey)
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
        get() = readStoredResponse()

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
