package com.revenuecat.purchases.common.diagnostics

internal enum class DiagnosticsEventName {
    HTTP_REQUEST_PERFORMED,
    MAX_EVENTS_STORED_LIMIT_REACHED,
    GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST,
    GOOGLE_QUERY_PURCHASES_REQUEST,
    GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
    PRODUCT_DETAILS_NOT_SUPPORTED,
    CUSTOMER_INFO_VERIFICATION_RESULT,
    AMAZON_QUERY_PRODUCT_DETAILS_REQUEST,
    AMAZON_QUERY_PURCHASES_REQUEST,
}
