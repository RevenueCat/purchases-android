package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.LocaleProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject

@OptIn(InternalRevenueCatAPI::class)
internal data class CachedOfferingsResponse(
    val response: JSONObject,
    val originalSource: HTTPResponseOriginalSource,
)

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

    @Synchronized
    fun clearCache() {
        offeringsCachedObject.clearCache()
        deviceCache.clearOfferingsResponseCache()
        cachedLanguageTags = null
    }

    @Synchronized
    fun cacheOfferingsInMemory(offerings: Offerings) = updateInMemoryCache(offerings)

    @Synchronized
    fun cacheOfferings(offerings: Offerings, offeringsResponse: String) {
        updateInMemoryCache(offerings)
        deviceCache.cacheOfferingsResponse(offeringsResponse, offerings.originalSource)
    }

    private fun updateInMemoryCache(offerings: Offerings) {
        offeringsCachedObject.cacheInstance(offerings)
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

    val cachedOfferingsResponse: CachedOfferingsResponse?
        @Synchronized
        get() {
            val response = deviceCache.getOfferingsResponseCache() ?: return null
            val sourceName = deviceCache.getOfferingsResponseSource()
                ?: response.optNullableString(ORIGINAL_SOURCE_KEY)
                ?: HTTPResponseOriginalSource.MAIN.name
            val originalSource = try {
                HTTPResponseOriginalSource.valueOf(sourceName)
            } catch (e: IllegalArgumentException) {
                errorLog(e) { "Invalid original data source for cached offerings" }
                HTTPResponseOriginalSource.MAIN
            }
            return CachedOfferingsResponse(response, originalSource)
        }

    // endregion Offerings response cache
}
