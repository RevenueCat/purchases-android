package com.revenuecat.purchases.strings

import com.revenuecat.purchases.APIKeyValidator
import com.revenuecat.purchases.Store

internal object OfferingStrings {
    const val CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR = "Could not find ProductDetails for %s " +
        "\nThere is a problem with your configuration in Play Store Developer Console. " +
        "More info here: https://errors.rev.cat/configuring-products"
    const val FETCHING_OFFERINGS_ERROR = "Error fetching offerings - %s"
    const val FETCHING_PRODUCTS = "Requesting products from the store with identifiers: %s"
    const val FETCHING_PRODUCTS_FINISHED = "Products request finished for %s"
    const val BUILDING_OFFERINGS = "Building offerings response with %d products"
    const val CREATED_OFFERINGS = "Offerings object created with %d offerings"
    const val JSON_EXCEPTION_ERROR = "JSONException when building Offerings object. Message: %s"
    const val LIST_PRODUCTS = "%s - %s"
    const val LIST_UNFETCHED_PRODUCTS = "Product not found: %s - Product Type: %s, Reason: %s, Serialized doc ID: %s"
    const val EXTRA_QUERY_PRODUCT_DETAILS_RESPONSE = "BillingClient queryProductDetails has returned more than once, " +
        "with result: %s. More info here: https://rev.cat/google-duplicated-listener-timeouts"
    const val EXTRA_QUERY_PURCHASES_RESPONSE = "BillingClient queryPurchases has returned more than once, " +
        "with result: %s."
    const val EXTRA_GET_BILLING_CONFIG_RESPONSE = "BillingClient getBillingConfigAsync has returned more than once, " +
        "with result: %s."
    const val NO_CACHED_OFFERINGS_FETCHING_NETWORK = "No cached Offerings, fetching from network"
    const val FORCE_OFFERINGS_FETCHING_NETWORK = "Latest Offerings requested, fetching from network"
    const val OFFERINGS_STALE_UPDATING_IN_BACKGROUND = "Offerings cache is stale, updating from network in background"
    const val OFFERINGS_STALE_UPDATING_IN_FOREGROUND = "Offerings cache is stale, updating from network in foreground"
    const val OFFERINGS_START_UPDATE_FROM_NETWORK = "Start Offerings update from network."
    const val RETRIEVED_PRODUCTS = "Retrieved productDetailsList: %s"
    const val MISSING_PRODUCT_DETAILS = "Missing productDetails: %s"
    const val VENDING_OFFERINGS_CACHE = "Vending Offerings from cache"
    const val EMPTY_PRODUCT_ID_LIST = "productId list is empty, skipping queryProductDetailsAsync call"
    public fun getConfigurationErrorNoProductsForOfferings(
        apiKeyValidationResult: APIKeyValidator.ValidationResult,
        configuredStore: Store,
    ): String {
        val storeNameForLogging = apiKeyValidationResult.storeNameForLogging(configuredStore)
        val description = if (storeNameForLogging != null) {
            val indefiniteArticle = apiKeyValidationResult.indefiniteArticle(configuredStore)
            "You have configured the SDK with $indefiniteArticle $storeNameForLogging API key, " +
                "but there are no $storeNameForLogging products registered in the " +
                "RevenueCat dashboard for your offerings."
        } else {
            "You have configured the SDK with an API key from a store that has no products " +
                "registered in the RevenueCat dashboard for your offerings."
        }
        return description + " If you don't want to use the offerings system, you can safely ignore this message. " +
            "To configure offerings and their products, follow the instructions in " +
            "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
    }
    const val CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND = "There's a problem with your configuration. " +
        "None of the products registered in the RevenueCat dashboard could be fetched from the Play Store.\n" +
        "More information: https://rev.cat/why-are-offerings-empty"
    const val OFFERING_EMPTY = "There's a problem with your configuration. No packages could be found for offering " +
        "with identifier %s. This could be due to Products not being configured correctly in " +
        "the RevenueCat dashboard or Play Store.\nTo configure products, follow the instructions in " +
        "https://rev.cat/how-to-configure-offerings.\nMore information: https://rev.cat/why-are-offerings-empty"
    const val ERROR_FETCHING_OFFERINGS_USING_DISK_CACHE = "Error fetching offerings. Using disk cache."
    const val TARGETING_ERROR = "Error while parsing targeting - skipping"
}

private fun APIKeyValidator.ValidationResult.storeNameForLogging(configuredStore: Store): String? {
    return when (this) {
        APIKeyValidator.ValidationResult.VALID -> when (configuredStore) {
            Store.PLAY_STORE -> "Play Store"
            Store.AMAZON -> "Amazon Appstore"
            else -> null
        }
        APIKeyValidator.ValidationResult.LEGACY -> "Play Store"
        APIKeyValidator.ValidationResult.SIMULATED_STORE -> "Test Store"
        APIKeyValidator.ValidationResult.OTHER_PLATFORM,
        APIKeyValidator.ValidationResult.GOOGLE_KEY_AMAZON_STORE,
        APIKeyValidator.ValidationResult.AMAZON_KEY_GOOGLE_STORE,
        -> null
    }
}

private fun APIKeyValidator.ValidationResult.indefiniteArticle(configuredStore: Store): String {
    return when (this) {
        APIKeyValidator.ValidationResult.VALID -> when (configuredStore) {
            Store.PLAY_STORE -> "a" // "a Play Store API key"
            Store.AMAZON -> "an" // "an Amazon Appstore API key"
            else -> "a"
        }
        APIKeyValidator.ValidationResult.LEGACY -> "a" // "a Play Store API key"
        APIKeyValidator.ValidationResult.SIMULATED_STORE -> "a" // "a Test Store API key"
        APIKeyValidator.ValidationResult.OTHER_PLATFORM,
        APIKeyValidator.ValidationResult.GOOGLE_KEY_AMAZON_STORE,
        APIKeyValidator.ValidationResult.AMAZON_KEY_GOOGLE_STORE,
        -> "a"
    }
}
