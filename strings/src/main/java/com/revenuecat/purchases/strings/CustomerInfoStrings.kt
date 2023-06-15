package com.revenuecat.purchases.strings

object CustomerInfoStrings {
    const val INVALIDATING_CUSTOMERINFO_CACHE = "Invalidating CustomerInfo cache."
    const val NO_CACHED_CUSTOMERINFO = "No cached CustomerInfo, fetching from network."
    const val CUSTOMERINFO_STALE_UPDATING_BACKGROUND = "CustomerInfo cache is stale, updating from " +
        "network in background."
    const val CUSTOMERINFO_STALE_UPDATING_FOREGROUND = "CustomerInfo cache is stale, updating from " +
        "network in foreground."
    const val CUSTOMERINFO_UPDATED_FROM_NETWORK = "CustomerInfo updated from network."
    const val CUSTOMERINFO_UPDATED_FROM_SYNCING_PENDING_PURCHASES = "CustomerInfo updated syncing pending purchases."
    const val CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER = "CustomerInfo updated, sending to listener."
    const val SENDING_LATEST_CUSTOMERINFO_TO_LISTENER = "Sending latest CustomerInfo to listener."
    const val VENDING_CACHE = "Vending CustomerInfo from cache."
    const val RETRIEVING_CUSTOMER_INFO = "Retrieving customer info with policy: %s"
    const val MISSING_CACHED_CUSTOMER_INFO = "Requested a cached CustomerInfo but it's not available."
    const val ERROR_FETCHING_CUSTOMER_INFO = "Error fetching customer data: %s."
    const val COMPUTING_OFFLINE_CUSTOMER_INFO_FAILED = "Error computing offline CustomerInfo. " +
        "Will return original error. Creation error: %s"
}
