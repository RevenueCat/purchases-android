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

    private var pendingResponse: String? = null
    private var diskGeneration = 0L
    private val diskWriteLock = Any()

    init {
        deviceCache.removeLegacyOfferingsResponse()
    }

    @Synchronized
    fun clearCache() {
        offeringsCachedObject.clearCache()
        diskGeneration++
        pendingResponse = null
        deviceCache.clearOfferingsResponseChecksum()
        responseStore.delete(responseKey)
        cachedLanguageTags = null
    }

    /**
     * [responsePayload] is null when [offerings] was parsed from the disk cache, whose response is already
     * on disk: re-writing it would rewrite a multi-MB file on every failed fetch.
     */
    @Synchronized
    fun cacheOfferings(offerings: Offerings, responsePayload: String?) {
        offeringsCachedObject.cacheInstance(offerings)
        if (responsePayload != null) {
            pendingResponse = responsePayload
            // Off the caller's thread: offerings are cached from the store's product query callback.
            dispatcher.enqueue({ persistPendingResponse() })
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
        get() = deviceCache.offeringsResponseCacheKey

    private fun persistPendingResponse() {
        synchronized(diskWriteLock) {
            val (responsePayload, generation) = synchronized(this) {
                val payload = pendingResponse ?: return
                pendingResponse = null
                payload to diskGeneration
            }
            val checksum = responsePayload
                .takeUnless { isAlreadyStored(it) }
                ?.let { responseStore.write(responseKey, it) }
                ?: return
            synchronized(this) {
                if (generation == diskGeneration) {
                    deviceCache.cacheOfferingsResponseChecksum(checksum)
                } else {
                    responseStore.delete(responseKey)
                }
            }
        }
    }

    // A 304 from the ETag cache hands back the same payload on every refresh.
    private fun isAlreadyStored(responsePayload: String): Boolean {
        val storedChecksum = deviceCache.getOfferingsResponseChecksum() ?: return false
        return responseStore.checksumOf(responsePayload) == storedChecksum && responseStore.contains(responseKey)
    }

    // region Offerings response cache

    val cachedOfferingsResponse: JSONObject?
        @Suppress("SwallowedException")
        get() {
            val response = readStoredResponse() ?: return null
            return try {
                JSONObject(response)
            } catch (exception: JSONException) {
                null
            }
        }

    private fun readStoredResponse(): String? {
        val checksum = deviceCache.getOfferingsResponseChecksum() ?: return null
        return responseStore.read(responseKey, checksum) ?: null.also { forgetUnreadableResponse(checksum) }
    }

    // Lets the next unchanged response rewrite the file instead of being skipped by isAlreadyStored.
    @Synchronized
    private fun forgetUnreadableResponse(checksum: Long) {
        if (deviceCache.getOfferingsResponseChecksum() == checksum) {
            deviceCache.clearOfferingsResponseChecksum()
        }
    }

    // endregion Offerings response cache
}
