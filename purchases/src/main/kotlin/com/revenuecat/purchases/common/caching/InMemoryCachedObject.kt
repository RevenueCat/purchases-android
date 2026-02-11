package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import java.util.Date

internal class InMemoryCachedObject<T>(
    internal var lastUpdatedAt: Date? = null,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    public var cachedInstance: T? = null

    public fun clearCacheTimestamp() {
        lastUpdatedAt = null
    }

    public fun clearCache() {
        clearCacheTimestamp()
        cachedInstance = null
    }

    public fun cacheInstance(instance: T) {
        this.cachedInstance = instance
        this.lastUpdatedAt = dateProvider.now
    }

    public fun updateCacheTimestamp(date: Date) {
        this.lastUpdatedAt = date
    }
}
