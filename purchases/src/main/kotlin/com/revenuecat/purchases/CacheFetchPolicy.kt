package com.revenuecat.purchases

/**
 * Specifies behavior for a caching API. Current values:
 * - CACHE_ONLY: Returns values from the cache, or throws an error if not available. It won't initiate a fetch.
 * - FETCH_CURRENT: Ignore whether the cache has a value or not and fetch the most up-to-date data. This will return an
 *  error if the fetch request fails
 * - NOT_STALE_CACHED_OR_CURRENT: Returns the cached data if available and not stale. If not available or stale,
 *  it fetches up-to-date data. Note that this won't return stale data even if fetch request fails
 * - CACHED_OR_FETCHED: (default) returns the cached data if available (even if stale). If not available, fetches
 *  up-to-date data. If cached data is staled, it initiates a fetch in the background.
 */
enum class CacheFetchPolicy {
    CACHE_ONLY,
    FETCH_CURRENT,
    NOT_STALE_CACHED_OR_CURRENT,
    CACHED_OR_FETCHED;

    companion object {
        fun default() = CACHED_OR_FETCHED
    }
}
