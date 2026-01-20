package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.networking.HTTPResult

/**
 * Indicates where the data originally came from (before caching).
 */
@InternalRevenueCatAPI
enum class HTTPResponseOriginalSource {
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
}

/**
 * Converts HTTPResult to OriginalDataSource based on response characteristics.
 */
@OptIn(InternalRevenueCatAPI::class)
internal val HTTPResult.originalDataSource: HTTPResponseOriginalSource
    get() {
        if (isLoadShedderResponse && isFallbackURL) {
            errorLog {
                "Request to fallback URL was handled by load shedder, which should never happen. " +
                    "Defaulting to fallback source."
            }
        }
        return when {
            isFallbackURL -> HTTPResponseOriginalSource.FALLBACK
            isLoadShedderResponse -> HTTPResponseOriginalSource.LOAD_SHEDDER
            else -> HTTPResponseOriginalSource.MAIN
        }
    }
