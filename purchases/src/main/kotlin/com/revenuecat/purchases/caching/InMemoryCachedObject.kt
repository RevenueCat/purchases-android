package com.revenuecat.purchases.caching

import java.util.Date

internal class InMemoryCachedObject<T> {

    var lastUpdatedAt: Date?
    val cacheDurationInSeconds: Int
    var cachedInstance: T? = null

    constructor(cacheDurationInSeconds: Int) : this(null, cacheDurationInSeconds)

    constructor(lastUpdatedAt: Date?, cacheDurationInSeconds: Int) {
        this.lastUpdatedAt = lastUpdatedAt
        this.cacheDurationInSeconds = cacheDurationInSeconds
    }

    fun isCacheStale(): Boolean {
        return lastUpdatedAt?.let { cachesLastUpdated ->
            Date().time - cachesLastUpdated.time >= cacheDurationInSeconds
        }?: true
    }

    fun clearCacheTimestamp() {
        lastUpdatedAt = null
    }

    fun clearCache() {
        clearCacheTimestamp()
        cachedInstance = null
    }

    fun cacheInstance(instance: T, date: Date) {
        this.cachedInstance = instance
        this.lastUpdatedAt = date
    }

    fun updateCacheTimestamp(date: Date) {
        this.lastUpdatedAt = date
    }
}