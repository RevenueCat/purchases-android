package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.HTTPResult

/**
 * Indicates where the data was obtained from.
 * Used for tracking the source of Offerings and ProductEntitlementMapping.
 */
internal enum class DataSource {
    /**
     * Data obtained from normal network fetch.
     */
    MAIN,

    /**
     * Data obtained from a response that included the `x-revenuecat-fortress` header set to `true`.
     */
    LOAD_SHEDDER,

    /**
     * Data obtained from a fallback base URL when the main request failed.
     */
    FALLBACK,

    /**
     * Data obtained from cache.
     */
    CACHE,
}

/**
 * Indicates where the data originally came from (before caching).
 * Used to preserve the original source when data is retrieved from cache.
 */
internal enum class OriginalDataSource {
    /**
     * Original data came from normal network fetch.
     */
    MAIN,

    /**
     * Original data came from a response that included the `x-revenuecat-fortress` header set to `true`.
     */
    LOAD_SHEDDER,

    /**
     * Original data came from a fallback base URL when the main request failed.
     */
    FALLBACK,
    ;

    fun asDataSource(): DataSource {
        return when (this) {
            MAIN -> DataSource.MAIN
            LOAD_SHEDDER -> DataSource.LOAD_SHEDDER
            FALLBACK -> DataSource.FALLBACK
        }
    }
}

/**
 * Converts HTTPResult to OriginalDataSource based on response characteristics.
 */
internal val HTTPResult.originalDataSource: OriginalDataSource
    get() = when {
        isLoadShedderResponse == true -> OriginalDataSource.LOAD_SHEDDER
        isFallbackURL == true -> OriginalDataSource.FALLBACK
        else -> OriginalDataSource.MAIN
    }
