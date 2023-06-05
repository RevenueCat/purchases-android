package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.CacheFetchPolicy

@Suppress("unused", "UNUSED_VARIABLE")
private class CacheFetchPolicyAPI {

    fun check(fetchPolicy: CacheFetchPolicy) {
        when (fetchPolicy) {
            CacheFetchPolicy.CACHE_ONLY,
            CacheFetchPolicy.FETCH_CURRENT,
            CacheFetchPolicy.CACHED_OR_FETCHED,
            CacheFetchPolicy.NOT_STALE_CACHED_OR_CURRENT,
            -> {}
        }.exhaustive
    }
}
