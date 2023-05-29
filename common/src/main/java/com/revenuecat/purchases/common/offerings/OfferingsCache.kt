package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import org.json.JSONObject

class OfferingsCache(
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val offeringsCachedObject: InMemoryCachedObject<Offerings> = InMemoryCachedObject(
        dateProvider = dateProvider
    )
) {

    @Synchronized
    fun clearCache() {
        offeringsCachedObject.clearCache()
        deviceCache.clearOfferingsResponseCache()
    }

    @Synchronized
    fun cacheOfferings(offerings: Offerings, offeringsResponse: JSONObject) {
        offeringsCachedObject.cacheInstance(offerings)
        deviceCache.cacheOfferingsResponse(offeringsResponse)
    }

    // region Offerings cache

    val cachedOfferings: Offerings?
        @Synchronized
        get() = offeringsCachedObject.cachedInstance

    @Synchronized
    fun isOfferingsCacheStale(appInBackground: Boolean): Boolean {
        return offeringsCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider)
    }

    @Synchronized
    fun clearOfferingsCacheTimestamp() {
        offeringsCachedObject.clearCacheTimestamp()
    }

    @Synchronized
    fun setOfferingsCacheTimestampToNow() {
        offeringsCachedObject.updateCacheTimestamp(dateProvider.now)
    }

    // endregion Offerings cache

    // region Offerings response cache

    val cachedOfferingsResponse: JSONObject?
        @Synchronized
        get() = deviceCache.getOfferingsResponseCache()

    // endregion Offerings response cache
}
