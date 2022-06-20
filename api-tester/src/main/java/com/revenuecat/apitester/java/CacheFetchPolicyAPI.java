package com.revenuecat.apitester.java;

import com.revenuecat.purchases.CacheFetchPolicy;

@SuppressWarnings({"unused"})
final class CacheFetchPolicyAPI {
    static void check(final CacheFetchPolicy fetchPolicy) {
        switch (fetchPolicy) {
            case CACHE_ONLY:
            case FETCH_CURRENT:
            case CACHED_OR_FETCHED:
            case NOT_STALE_CACHED_OR_CURRENT:
        }
    }
}
