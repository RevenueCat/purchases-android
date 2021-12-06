package com.revenuecat.purchases.strings

object CustomerInfoStrings {
    const val INVALIDATING_CUSTOMERINFO_CACHE = "Invalidating CustomerInfo cache."
    const val NO_CACHED_CUSTOMERINFO = "No cached CustomerInfo, fetching from network."
    const val CUSTOMERINFO_STALE_UPDATING_BACKGROUND = "CustomerInfo cache is stale, updating from " +
            "network in background."
    const val CUSTOMERINFO_STALE_UPDATING_FOREGROUND = "CustomerInfo cache is stale, updating from " +
            "network in foreground."
    const val CUSTOMERINFO_UPDATED_FROM_NETWORK = "CustomerInfo updated from network."
    const val CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER = "CustomerInfo updated, sending to listener."
    const val SENDING_LATEST_CUSTOMERINFO_TO_LISTENER = "Sending latest CustomerInfo to listener."
    const val VENDING_CACHE = "Vending CustomerInfo from cache."
}
