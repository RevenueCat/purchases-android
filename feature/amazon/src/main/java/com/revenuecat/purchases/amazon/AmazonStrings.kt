package com.revenuecat.purchases.amazon

object AmazonStrings {
    const val REQUESTING_PRODUCTS = "Requesting Amazon products with identifiers: %s"
    const val PRODUCT_CHANGES_NOT_SUPPORTED = "Amazon doesn't support product changes"
    const val PRODUCTS_REQUEST_FINISHED = "Products request finished with result %s"
    const val USER_DATA_REQUEST_FINISHED = "User data request finished with result %s"
    const val PRODUCTS_REQUEST_UNAVAILABLE = "Unavailable products: %s"
    const val RETRIEVED_PRODUCT_DATA = "Retrieved productData: %s"
    const val RETRIEVED_PRODUCT_DATA_EMPTY = "Product data is empty"
    const val PURCHASE_REQUEST_FINISHED = "Purchase request finished: \n %s"
    const val ERROR_PURCHASE_UNKNOWN = "Failed to make purchase. There was an Amazon store problem."
    const val ERROR_PURCHASE_NOT_SUPPORTED = "Failed to make purchase. Call is not supported"
    const val ERROR_PURCHASE_ALREADY_OWNED = "Failed to make purchase. User already owns SKU."
    const val ERROR_PURCHASE_INVALID_SKU = "Failed to make purchase. SKU is invalid"
    const val ERROR_PURCHASE_FAILED = "Failed to make purchase. " +
        "This error normally means that the purchase was cancelled"
    const val ERROR_FETCHING_RECEIPT_INFO = "There was an error fetching receipt information: %s"
    const val ERROR_FETCHING_RECEIPTS = "Couldn't fetch information for the following receipts: \n %s"
    const val ERROR_FETCHING_PURCHASE_HISTORY_ALL_RECEIPTS_INVALID =
        "Error fetching purchase history. All receipts are invalid."
    const val CACHING_RECEIPT_TERM_SKUS = "Caching term skus for receipts: \n %s"
    const val RECEIPT_DATA_RECEIVED = "Received receipt data: \n %s"
    const val ERROR_FINDING_RECEIPT_SKU = "Couldn't find sku for token %s"
    const val ERROR_FAILED_PURCHASES_UPDATES = "Failed to get purchase updates."
    const val ERROR_UNSUPPORTED_PURCHASES_UPDATES =
        "Failed to get purchase updates. Call is not supported."
    const val ERROR_PURCHASES_UPDATES_STORE_PROBLEM =
        "Failed to get purchase updates. There was an Amazon store problem."
    const val ERROR_FAILED_USER_DATA = "Failed to get user data."
    const val ERROR_UNSUPPORTED_USER_DATA =
        "Failed to get user data. Call is not supported."
    const val ERROR_USER_DATA_STORE_PROBLEM =
        "Failed to get user data. There was an Amazon store problem."
    const val PRICE_EXTRACTION_ITERATING_OVER_AVAILABLE_CURRENCIES = "Currency symbol from Amazon price is not the " +
        "same as Locale's. Iterating over all available currencies to find the currency code."
    const val PRICE_EXTRACTION_USING_CURRENCY_SYMBOL = "Couldn't determine currencyCode. Setting currencyCode to " +
        "symbol sent by Amazon"
    const val PRICE_EXTRACTION_PARSE_EXCEPTION = "Exception parsing number. Price is %s"
    const val ERROR_OBSERVER_MODE_NOT_SUPPORTED =
        "Attempting to interact with Amazon App Store with an Amazon Purchases configuration in observer mode, " +
            "but observer mode is not yet supported."
}
