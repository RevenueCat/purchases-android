package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.constants.HelperDefine

internal object GalaxyStrings {
    // Configuration
    const val SETTING_OPERATION_MODE = "Setting the Galaxy Store's operation mode to %s."
    const val STOREFRONT_NOT_SUPPORTED = "Fetching the storefront is not supported for the Galaxy Store."

    // Product Fetching
    const val EMPTY_GET_PRODUCT_DETAILS_REQUEST = "Received a request for 0 products. Returning an empty list."
    const val ANOTHER_GET_PRODUCT_DETAILS_REQUEST_IN_FLIGHT = "A request to fetch products from the Galaxy Store" +
        " is already in progress. Please wait until that request completes and then try again."
    const val REQUESTING_PRODUCTS = "Requesting Galaxy products with identifiers: %s"
    const val GET_PRODUCT_DETAILS_REQUEST_ERRORED = "An error occurred while fetching product details for product " +
        "IDs %s from the Galaxy Store. Error: %s"
    const val GET_PRODUCT_DETAILS_RESPONSE_MISSING_PRODUCTS = "The Galaxy Store returned product details for only " +
        "some of the requested IDs. Requested: %s. Missing: %s"

    // Product Parsing
    const val UNKNOWN_SUBSCRIPTION_DURATION_UNIT = "Detected an unknown SubscriptionDurationUnit from the " +
        "Galaxy Store: %s"
    const val CANNOT_PARSE_LEADING_INT_FROM_SUBSCRIPTION_DURATION_MULTIPLIER = "Cannot parse period value from the " +
        "SubscriptionDurationMultiple from the Galaxy Store: %s"
    const val UNKNOWN_GALAXY_IAP_TYPE_STRING = "Detected an unknown IAP Type string from the Galaxy Store: %s"
    const val CANNOT_PARSE_GALAXY_PRODUCT_SUBSCRIPTION_PERIOD = "Could not parse period for Galaxy Store " +
        "subscription. Product's subscriptionDurationMultiplier is %s"

    // Purchasing
    const val ANOTHER_PURCHASE_REQUEST_IN_FLIGHT = "Another purchase request with the Galaxy Store" +
        " is already in progress. Please wait until that request completes and then try again."
    const val GALAXY_OTPS_NOT_SUPPORTED = "Purchasing One-time purchases is not currently supported for the " +
        "Galaxy Store."
    const val GALAXY_STORE_FAILED_TO_ACCEPT_PAYMENT_REQUEST = "The Galaxy Store did not accept the IAP payment " +
        "request for processing. No transactions have occurred."
    const val PURCHASE_REQUEST_ERRORED = "An error occurred while purchasing product with ID %s with the " +
        "Galaxy Store. Error: %s"
    const val PURCHASE_RETURNED_SUCCESS_BUT_NO_PURCHASE_RESULT = "The purchase request returned no error, but also " +
        "returned no purchase result. This is likely an issue with the Galaxy Store."
    const val ERROR_CANNOT_PARSE_PURCHASE_RESULT = "Could not parse the purchase result for a Galaxy Store purchase. " +
        "This is likely an issue with the Galaxy Store. Error: %s."
    const val ERROR_CANNOT_PARSE_PURCHASE_DATE = "Could not parse purchase date for Galaxy Store purchase. Purchase " +
        "date string: %s"

    // Subscription Plan Changes
    const val ANOTHER_CHANGE_SUBSCRIPTION_PLAN_REQUEST_IN_FLIGHT = "Another subscription plan change request with " +
        "the Galaxy Store is already in progress. Please wait until that request completes and then try again."
    const val CHANGE_SUBSCRIPTION_PLAN_NO_OLD_PRODUCT_ID = "Cannot change subscription plan: the old purchase " +
        "does not have a product ID."
    const val GALAXY_STORE_FAILED_TO_ACCEPT_CHANGE_SUBSCRIPTION_PLAN_REQUEST = "The Galaxy Store did not accept " +
        "the subscription plan change request for processing."
    const val CHANGE_SUBSCRIPTION_PLAN_RETURNED_SUCCESS_BUT_NO_RESULT = "The subscription plan change request " +
        "returned no error, but also returned no result. This is likely an issue with the Galaxy Store."
    const val CHANGE_SUBSCRIPTION_PLAN_REQUEST_ERRORED = "An error occurred while changing subscription from " +
        "product ID %s to %s with the Galaxy Store. Error: %s"

    // Promotion Eligibility
    const val EMPTY_GET_PROMOTION_ELIGIBILITY_REQUEST = "Received a promotion eligibility request for 0 " +
        "product IDs. Returning an empty list."
    const val ANOTHER_GET_PROMOTION_ELIGIBILITY_REQUEST_IN_FLIGHT = "A request to fetch promotion eligibility " +
        "from the Galaxy Store is already in progress. Please wait until that request completes and then try again."
    const val REQUESTING_PROMOTION_ELIGIBILITY = "Requesting promotion eligibility from the Galaxy Store for products" +
        " with identifiers: %s"
    const val GALAXY_STORE_FAILED_TO_ACCEPT_PROMOTION_ELIGIBILITY_REQUEST = "The Galaxy Store did not " +
        "accept the promotion eligibility request for processing."
    const val PROMOTION_ELIGIBILITY_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS = "The promotion eligibility" +
        " request returned no error, but also returned no eligibility results. This is likely an issue with the " +
        "Galaxy Store."
    const val PROMOTION_ELIGIBILITY_REQUEST_ERRORED = "An error occurred while fetching promotion eligibility " +
        "for product IDs %s from the Galaxy Store. Error: %s"

    // Acknowledging Purchases
    const val WARNING_ACKNOWLEDGING_PURCHASES_UNAVAILABLE = "Acknowledging purchases is currently unavailable."
    const val ANOTHER_ACKNOWLEDGE_REQUEST_IN_FLIGHT = "Another acknowledge purchase request with the Galaxy Store" +
        " is already in progress. Please wait until that request completes and then try again."
    const val GALAXY_STORE_FAILED_TO_ACCEPT_ACKNOWLEDGE_REQUEST = "The Galaxy Store did not accept the acknowledge " +
        "IAP request for processing."
    const val ACKNOWLEDGE_REQUEST_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS = "The acknowledgement request " +
        "returned no error, but also returned no acknowledgement results. This is likely an issue with the " +
        "Galaxy Store."
    const val ACKNOWLEDGE_REQUEST_RETURNED_MORE_THAN_ONE_RESULT = "The acknowledgement request returned " +
        "more than one acknowledgement result, only acknowledging one purchase. This is likely an issue " +
        "with the Galaxy Store."
    const val ACKNOWLEDGE_REQUEST_ERRORED = "An error occurred while acknowledging product with token %s with the " +
        "Galaxy Store. Error: %s"
    const val ACKNOWLEDGE_REQUEST_RETURNED_UNKNOWN_STATUS_CODE = "The acknowledgement request returned " +
        "an unknown status code %s."
    const val ACKNOWLEDGE_REQUEST_RETURNED_ERROR_STATUS_CODE = "The acknowledgement request returned " +
        "an error status code %s (%s)."
    const val NOT_ACKNOWLEDGING_TRANSACTION_BECAUSE_ALREADY_ACKNOWLEDGED = "Will not acknowledge transaction with " +
        "product ID %s because it has already been acknowledged."

    // Get Owned Products
    const val ANOTHER_GET_OWNED_LIST_REQUEST_IN_FLIGHT = "Another get owned products request with the Galaxy Store" +
        " is already in progress. Please wait until that request completes and then try again."
    const val REQUESTING_OWNED_LIST = "Requesting owned products from the Galaxy Store."
    const val GALAXY_STORE_FAILED_TO_ACCEPT_OWNED_LIST_REQUEST = "The Galaxy Store did not accept the owned products " +
        "request for processing."
    const val GET_OWNED_LIST_REQUEST_ERRORED = "An error occurred while getting the owned products " +
        "from the Galaxy Store. Error: %s"

    // Misc
    const val CREATING_PURCHASES_ERROR_FOR_GALAXY_ERROR_NONE = "Creating a PurchasesError for a Galaxy Store error" +
        " with an error of IAP_ERROR_NONE."
    const val WARNING_CANNOT_CONSUME_NON_SUBS_PRODUCT_TYPES = "Acknowledging/consuming non-subscription " +
        "product types is currently unsupported."
}

internal fun HelperDefine.OperationMode.description(): String = when (this) {
    HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION -> "PRODUCTION"
    HelperDefine.OperationMode.OPERATION_MODE_TEST -> "TEST"
    HelperDefine.OperationMode.OPERATION_MODE_TEST_FAILURE -> "TEST_FAILURE"
}
