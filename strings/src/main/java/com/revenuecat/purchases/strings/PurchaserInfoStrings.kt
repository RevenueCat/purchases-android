package com.revenuecat.purchases.strings

object PurchaserInfoStrings {
    const val INITIAL_PURCHASERINFO = Emojis.INFO + " PurchaserInfo updated, sending to listener."
    const val INVALIDATE_PURCHASERINFO_CACHE = Emojis.INFO + " Invalidating PurchaserInfo cache."
    const val LATEST_PURCHASERINFO = Emojis.INFO + " Sending latest PurchaserInfo to listener."
    const val NO_CACHE = Emojis.INFO + " No cached PurchaserInfo, fetching from network."
    const val PURCHASERINFO_STALE_FOREGROUND = Emojis.INFO + " PurchaserInfo cache is stale, " +
            "updating from network in foreground"
    const val STALE_CACHE_BACKGROUND = Emojis.INFO + " PurchaserInfo cache is stale, " +
            "updating from network in background."
    const val UPDATED_FROM_NETWORK = Emojis.HEART_CAT_EYES + " PurchaserInfo updated from network."
    const val VENDING_CACHE = Emojis.INFO + " Vending PurchaserInfo from cache."
}
