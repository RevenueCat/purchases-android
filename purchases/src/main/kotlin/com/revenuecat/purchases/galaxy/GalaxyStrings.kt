package com.revenuecat.purchases.galaxy

internal object GalaxyStrings {
    const val STOREFRONT_NOT_SUPPORTED = "Fetching the storefront is not supported for the Galaxy Store."

    // Product Fetching
    const val EMPTY_GET_PRODUCT_DETAILS_REQUEST = "Received a request for 0 products. Returning an empty list."
    const val ANOTHER_GET_PRODUCT_DETAILS_REQUEST_IN_FLIGHT = "A request to fetch products from the Galaxy Store" +
        "is already in progress. Please wait until that request completes and then try again."
    const val REQUESTING_PRODUCTS = "Requesting Galaxy products with identifiers: %s"
    const val ERROR_TIMEOUT_GETTING_PRODUCT_DETAILS = "Timeout error trying to get Galaxy product details for " +
        "products: %s. Please check that the product IDs are correct."
    const val GET_PRODUCT_DETAILS_REQUEST_ERRORED = "An error occurred while fetching product details for product IDs" +
        "%s from the Galaxy Store. Error: %s"

    // Product Parsing
    const val UNKNOWN_SUBSCRIPTION_DURATION_UNIT = "Detected an unknown SubscriptionDurationUnit from the " +
        "Galaxy Store: %s"
    const val CANNOT_PARSE_LEADING_INT_FROM_SUBSCRIPTION_DURATION_MULTIPLIER = "Cannot parse period value from the " +
        "SubscriptionDurationMultiple from the Galaxy Store: %s"
    const val UNKNOWN_GALAXY_IAP_TYPE_STRING = "Detected an unknown IAP Type string from the Galaxy Store: %s"
    const val CANNOT_PARSE_GALAXY_PRODUCT_SUBSCRIPTION_PERIOD = "Could not parse period for Galaxy Store subscription. " +
        "Product's subscriptionDurationMultiplier is %s"
}
