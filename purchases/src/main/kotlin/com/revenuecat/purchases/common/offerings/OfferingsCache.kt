package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import org.json.JSONObject

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

    @Synchronized
    fun cacheOfferings(offerings: Offerings, offeringsResponse: JSONObject) {
        offeringsCachedObject.cacheInstance(offerings)
        deviceCache.cacheOfferingsResponse(offeringsResponse)
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
