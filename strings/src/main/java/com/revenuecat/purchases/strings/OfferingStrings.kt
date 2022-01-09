package com.revenuecat.purchases.strings

object OfferingStrings {
    const val CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR = "Could not find SkuDetails for %s " +
        "\nThere is a problem with your configuration in Play Store Developer Console. " +
        "More info here: https://errors.rev.cat/configuring-products"
    const val FETCHING_OFFERINGS_ERROR = "Error fetching offerings - %s"
    const val FETCHING_PRODUCTS = "Requesting products from the store with identifiers: %s"
    const val FETCHING_PRODUCTS_ERROR = "Error when fetching products %s"
    const val FETCHING_PRODUCTS_FINISHED = "Products request finished for %s"
    const val JSON_EXCEPTION_ERROR = "JSONException when building Offerings object. Message: %s"
    const val LIST_PRODUCTS = "%s - %s"
    const val EXTRA_QUERY_SKU_DETAILS_RESPONSE = "BillingClient querySkuDetails has returned more than once, " +
        "with result: %s. More info here: https://rev.cat/google-duplicated-listener-timeouts"
    const val NO_CACHED_OFFERINGS_FETCHING_NETWORK = "No cached Offerings, fetching from network"
    const val OFFERINGS_STALE_UPDATING_IN_BACKGROUND = "Offerings cache is stale, updating from network in background"
    const val OFFERINGS_STALE_UPDATING_IN_FOREGROUND = "Offerings cache is stale, updating from network in foreground"
    const val OFFERINGS_UPDATED_FROM_NETWORK = "Offerings updated from network."
    const val RETRIEVED_PRODUCTS = "Retrieved skuDetailsList: %s"
    const val VENDING_OFFERINGS_CACHE = "Vending Offerings from cache"
    const val EMPTY_SKU_LIST = "SKU list is empty, skipping querySkuDetailsAsync call"
    const val CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS = "There's a problem with your configuration. " +
        "There are no products registered in the RevenueCat dashboard for your offerings. " +
        "To configure products, follow the instructions in " +
        "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
    const val CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND = "There's a problem with your configuration. " +
        "None of the products registered in the RevenueCat dashboard could be fetched from the Play Store.\n" +
        "More information: https://rev.cat/why-are-offerings-empty"
    const val OFFERING_EMPTY = "There's a problem with your configuration. No packages could be found for offering " +
        "with identifier %s. This could be due to Products not being configured correctly in " +
        "the RevenueCat dashboard or Play Store.\nTo configure products, follow the instructions in " +
        "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
}
