package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.utils.copy
import org.json.JSONObject

internal class OfferingsCache(
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        dateProvider = dateProvider,
    ),
    private val localeProvider: LocaleProvider,
) {
    companion object {
        const val ORIGINAL_SOURCE_KEY = "rc_original_source"
    }

    private var cachedLanguageTags: String? = null

    @Synchronized
    public fun clearCache() {
        offeringsCachedObject.clearCache()
        deviceCache.clearOfferingsResponseCache()
        cachedLanguageTags = null
    }

    @Synchronized
    public fun cacheOfferings(offerings: Offerings, offeringsResponse: JSONObject) {
        val finalJsonToCache = offeringsResponse.copy(deep = false).apply {
            put(ORIGINAL_SOURCE_KEY, offerings.originalSource)
        }
        offeringsCachedObject.cacheInstance(offerings)
        deviceCache.cacheOfferingsResponse(finalJsonToCache)
        offeringsCachedObject.updateCacheTimestamp(dateProvider.now)
        cachedLanguageTags = String(localeProvider.currentLocalesLanguageTags.toCharArray())
    }

    // region Offerings cache

    public val cachedOfferings: Offerings?
        @Synchronized
        get() = offeringsCachedObject.cachedInstance

    @Synchronized
    fun isOfferingsCacheStale(appInBackground: Boolean): Boolean =
        // Time-based staleness, or
        offeringsCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider) ||
            // Locale-based staleness
            cachedLanguageTags != localeProvider.currentLocalesLanguageTags

    @Synchronized
    fun forceCacheStale() {
        offeringsCachedObject.clearCacheTimestamp()
        cachedLanguageTags = null
    }

    // endregion Offerings cache

    // region Offerings response cache

    public val cachedOfferingsResponse: JSONObject?
        @Synchronized
        get() = deviceCache.getOfferingsResponseCache()

    // endregion Offerings response cache
}
