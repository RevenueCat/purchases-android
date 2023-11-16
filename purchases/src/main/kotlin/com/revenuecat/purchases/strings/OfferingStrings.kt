package com.revenuecat.purchases.strings

internal object OfferingStrings {
    const val CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR = "Could not find ProductDetails for %s " +
        "\nThere is a problem with your configuration in Play Store Developer Console. " +
        "More info here: https://errors.rev.cat/configuring-products"
    const val FETCHING_OFFERINGS_ERROR = "Error fetching offerings - %s"
    const val FETCHING_PRODUCTS = "Requesting products from the store with identifiers: %s"
    const val FETCHING_PRODUCTS_FINISHED = "Products request finished for %s"
    const val JSON_EXCEPTION_ERROR = "JSONException when building Offerings object. Message: %s"
    const val LIST_PRODUCTS = "%s - %s"
    const val EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE = "BillingClient queryProductDetails has returned more than once, " +
        "with result: %s. More info here: https://rev.cat/google-duplicated-listener-timeouts"
    const val EXTRA_QUERY_PURCHASES_RESPONSE = "BillingClient queryPurchases has returned more than once, " +
        "with result: %s."
    const val EXTRA_GET_BILLING_CONFIG_RESPONSE = "BillingClient getBillingConfigAsync has returned more than once, " +
        "with result: %s."
    const val NO_CACHED_OFFERINGS_FETCHING_NETWORK = "No cached Offerings, fetching from network"
    const val OFFERINGS_STALE_UPDATING_IN_BACKGROUND = "Offerings cache is stale, updating from network in background"
    const val OFFERINGS_STALE_UPDATING_IN_FOREGROUND = "Offerings cache is stale, updating from network in foreground"
    const val OFFERINGS_START_UPDATE_FROM_NETWORK = "Start Offerings update from network."
    const val RETRIEVED_PRODUCTS = "Retrieved productDetailsList: %s"
    const val VENDING_OFFERINGS_CACHE = "Vending Offerings from cache"
    const val EMPTY_PRODUCT_ID_LIST = "productId list is empty, skipping queryProductDetailsAsync call"
    const val CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS = "There are no products registered in the RevenueCat " +
        "dashboard for your offerings. If you don't want to use the offerings system, you can safely ignore this " +
        "message. To configure offerings and their products, follow the instructions in " +
        "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
    const val CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND = "There's a problem with your configuration. " +
        "None of the products registered in the RevenueCat dashboard could be fetched from the Play Store.\n" +
        "More information: https://rev.cat/why-are-offerings-empty"
    const val OFFERING_EMPTY = "There's a problem with your configuration. No packages could be found for offering " +
        "with identifier %s. This could be due to Products not being configured correctly in " +
        "the RevenueCat dashboard or Play Store.\nTo configure products, follow the instructions in " +
        "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
    const val ERROR_FETCHING_OFFERINGS_USING_DISK_CACHE = "Error fetching offerings. Using disk cache."
}
