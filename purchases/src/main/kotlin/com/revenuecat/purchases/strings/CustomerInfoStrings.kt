package com.revenuecat.purchases.strings

internal object CustomerInfoStrings {
    const val INVALIDATING_CUSTOMERINFO_CACHE = "Invalidating CustomerInfo cache."
    const val NO_CACHED_CUSTOMERINFO = "No cached CustomerInfo, fetching from network."
    const val NOT_CACHING_STALER_CUSTOMERINFO = "Received CustomerInfo is older than the cached one. " +
        "Won't be stored in cache."
    const val NOT_WAITING_FOR_UNSYNCED_PURCHASES = "Found unsynced purchases. Computing CustomerInfo on " +
        "device and posting them in the background, as configured by " +
        "UnsyncedTransactionsWaitPolicy.DO_NOT_WAIT."
    const val COMPUTING_CUSTOMERINFO_WITHOUT_WAITING_FAILED = "Couldn't compute CustomerInfo on device " +
        "(%s); waiting for unsynced purchases to be posted instead."
    const val CUSTOMERINFO_STALE_UPDATING_BACKGROUND = "CustomerInfo cache is stale, updating from " +
        "network in background."
    const val CUSTOMERINFO_STALE_UPDATING_FOREGROUND = "CustomerInfo cache is stale, updating from " +
        "network in foreground."
    const val CUSTOMERINFO_UPDATED_FROM_NETWORK = "CustomerInfo updated from network."
    const val CUSTOMERINFO_UPDATED_FROM_SYNCING_PENDING_PURCHASES = "CustomerInfo updated from syncing " +
        "pending purchases."
    const val CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER = "CustomerInfo updated, sending to listener."
    const val SENDING_LATEST_CUSTOMERINFO_TO_LISTENER = "Sending latest CustomerInfo to listener."
    const val VENDING_CACHE = "Vending CustomerInfo from cache."
    const val RETRIEVING_CUSTOMER_INFO = "Retrieving customer info with policy: %s"
    const val MISSING_CACHED_CUSTOMER_INFO = "Requested a cached CustomerInfo but it's not available."
    const val ERROR_FETCHING_CUSTOMER_INFO = "Error fetching customer data: %s."
    const val COMPUTING_OFFLINE_CUSTOMER_INFO_FAILED = "Error computing offline CustomerInfo. " +
        "Will return original error. Creation error: %s"
}
