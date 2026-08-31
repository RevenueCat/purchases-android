package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import org.json.JSONObject

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingsCache(
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        dateProvider = dateProvider,
    ),
    private val localeProvider: LocaleProvider,
) {
    private var cachedLanguageTags: String? = null

    @Synchronized
    fun clearCache() {
        offeringsCachedObject.clearCache()
        deviceCache.clearOfferingsResponseCache()
        cachedLanguageTags = null
    }

    /**
     * [responsePayload] is null when [offerings] was parsed from the disk cache, whose response is already
     * on disk: re-writing it would re-serialize a multi-MB payload on every failed fetch.
     */
    @Synchronized
    fun cacheOfferings(offerings: Offerings, responsePayload: String?) {
        offeringsCachedObject.cacheInstance(offerings)
        responsePayload?.let { deviceCache.cacheOfferingsResponse(it) }
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

    // region Offerings response cache

    val cachedOfferingsResponse: JSONObject?
        @Synchronized
        get() = deviceCache.getOfferingsResponseCache()

    // endregion Offerings response cache
}
