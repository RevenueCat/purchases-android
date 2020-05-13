package com.revenuecat.purchases.caching

import com.revenuecat.purchases.utils.DateProvider
import com.revenuecat.purchases.utils.DefaultDateProvider
import java.util.Date

internal class InMemoryCachedObject<T>(
    private val cacheDurationInSeconds: Int,
    internal var lastUpdatedAt: Date? = null,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {

    var cachedInstance: T? = null

    fun isCacheStale(): Boolean {
        return lastUpdatedAt?.let { cachesLastUpdated ->
            dateProvider.now.time - cachesLastUpdated.time >= cacheDurationInSeconds
        } ?: true
    }

    fun clearCacheTimestamp() {
        lastUpdatedAt = null
    }

    fun clearCache() {
        clearCacheTimestamp()
        cachedInstance = null
    }

    fun cacheInstance(instance: T) {
        this.cachedInstance = instance
        this.lastUpdatedAt = dateProvider.now
    }

    fun updateCacheTimestamp(date: Date) {
        this.lastUpdatedAt = date
    }
}
