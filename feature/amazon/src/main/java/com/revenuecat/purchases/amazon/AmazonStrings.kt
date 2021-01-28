package com.revenuecat.purchases.amazon

object AmazonStrings {
    const val REQUESTING_PRODUCTS = "Requesting Amazon products with identifiers: %s"
    const val PRODUCT_CHANGES_NOT_SUPPORTED = "Amazon doesn't support product changes"
    const val PRODUCTS_REQUEST_FINISHED = "Products request finished with result %s"
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
}
