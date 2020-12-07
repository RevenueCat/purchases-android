package com.revenuecat.purchases.strings

object PurchaserInfoStrings {
    const val PURCHASERINFO_UPDATED_NOTIFYING_LISTENER = "PurchaserInfo updated, sending to listener."
    const val INVALIDATING_PURCHASERINFO_CACHE = "Invalidating PurchaserInfo cache."
    const val SENDING_LATEST_PURCHASERINFO_TO_LISTENER = "Sending latest PurchaserInfo to listener."
    const val NO_CACHED_PURCHASERINFO = "No cached PurchaserInfo, fetching from network."
    const val PURCHASERINFO_STALE_UPDATE_FOREGROUND = "PurchaserInfo cache is stale, updating from " +
            "network in foreground."
    const val PURCHASERINFO_STALE_UPDATE_BACKGROUND = "PurchaserInfo cache is stale, updating from " +
            "network in background."
    const val UPDATED_FROM_NETWORK = "PurchaserInfo updated from network."
    const val VENDING_CACHE = "Vending PurchaserInfo from cache."
}
