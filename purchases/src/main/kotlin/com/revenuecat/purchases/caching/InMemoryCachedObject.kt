package com.revenuecat.purchases.caching

import com.revenuecat.purchases.utils.DateTimeProvider
import com.revenuecat.purchases.utils.DefaultDateTimeProvider
import java.util.Date

internal class InMemoryCachedObject<T>(
    private val cacheDurationInSeconds: Int,
    internal var lastUpdatedAt: Date? = null,
    private val dateTimeProvider: DateTimeProvider = DefaultDateTimeProvider()
) {

    var cachedInstance: T? = null

    fun isCacheStale(): Boolean {
        return lastUpdatedAt?.let { cachesLastUpdated ->
            dateTimeProvider.now.time - cachesLastUpdated.time >= cacheDurationInSeconds
        }?: true
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
        this.lastUpdatedAt = dateTimeProvider.now
    }

    fun updateCacheTimestamp(date: Date) {
        this.lastUpdatedAt = date
    }
}