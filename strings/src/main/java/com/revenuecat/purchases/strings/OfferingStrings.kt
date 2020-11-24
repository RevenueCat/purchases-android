package com.revenuecat.purchases.strings

object OfferingStrings {
    const val CANNOT_FIND_PRODUCT = "Could not find SkuDetails for %s"
    const val CONFIG_PROBLEM = "There is a problem with your configuration in Play Store Developer Console. " +
            "More info here: https://errors.rev.cat/configuring-products"
    const val FETCHING_OFFERINGS_ERROR = "Error fetching offerings - %s"
    const val FETCHING_PRODUCTS = "Requesting products from the store with identifiers: %s"
    const val FINISH_FETCHING_PRODUCTS = "Products request finished for %s"
    const val INVALID_PRODUCT_IDENTIFIERS = "Error when fetching products %s"
    const val JSON_EXCEPTION_ERROR = "JSONException when building Offerings object. Message: %s"
    const val NO_CACHED_OFFERINGS = "No cached Offerings, fetching from network"
    const val OFFERINGS_STALE_BACKGROUND = "Offerings cache is stale, updating from network in background"
    const val OFFERINGS_STALE_FOREGROUND = "Offerings cache is stale, updating from network in foreground"
    const val OFFERINGS_UPDATE_NETWORK = "Offerings updated from network."
    const val PRODUCTS = "%s - %s"
    const val RETRIEVED_SKU = "Retrieved skuDetailsList: %s"
    const val VENDING_OFFERINGS_CACHE = "Vending Offerings from cache"
}
