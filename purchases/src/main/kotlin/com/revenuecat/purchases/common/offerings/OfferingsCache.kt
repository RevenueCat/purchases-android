package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.utils.copy
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
    companion object {
        const val ORIGINAL_SOURCE_KEY = "rc_original_source"
    }

    private var cachedLanguageTags: String? = null

    // See WorkflowsCache: bumped on identity-transition clears so an in-flight fetch can't repopulate
    // the cleared cache after the user changed. cacheOfferings drops a write whose captured generation
    // no longer matches; the check and this increment share the intrinsic @Synchronized lock, so the
    // clear-then-write ordering is atomic.
    private var cacheGeneration: Int = 0

    @Synchronized
    fun currentGeneration(): Int = cacheGeneration

    @Synchronized
    fun clearCache() {
        cacheGeneration++
        offeringsCachedObject.clearCache()
        deviceCache.clearOfferingsResponseCache()
        cachedLanguageTags = null
    }

    @Synchronized
    fun cacheOfferings(offerings: Offerings, offeringsResponse: JSONObject, expectedGeneration: Int) {
        if (expectedGeneration != cacheGeneration) return
        val finalJsonToCache = offeringsResponse.copy(deep = false).apply {
            put(ORIGINAL_SOURCE_KEY, offerings.originalSource)
        }
        offeringsCachedObject.cacheInstance(offerings)
        deviceCache.cacheOfferingsResponse(finalJsonToCache)
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
