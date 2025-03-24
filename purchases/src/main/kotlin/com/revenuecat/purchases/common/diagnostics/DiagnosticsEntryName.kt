package com.revenuecat.purchases.common.diagnostics

internal enum class DiagnosticsEntryName {
    HTTP_REQUEST_PERFORMED,
    MAX_EVENTS_STORED_LIMIT_REACHED,
    MAX_DIAGNOSTICS_SYNC_RETRIES_REACHED,
    CLEARING_DIAGNOSTICS_AFTER_FAILED_SYNC,
    GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST,
    GOOGLE_QUERY_PURCHASES_REQUEST,
    GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST,
    GOOGLE_BILLING_START_CONNECTION,
    GOOGLE_BILLING_SETUP_FINISHED,
    GOOGLE_BILLING_SERVICE_DISCONNECTED,
    PRODUCT_DETAILS_NOT_SUPPORTED,
    CUSTOMER_INFO_VERIFICATION_RESULT,
    AMAZON_QUERY_PRODUCT_DETAILS_REQUEST,
    AMAZON_QUERY_PURCHASES_REQUEST,
    ENTERED_OFFLINE_ENTITLEMENTS_MODE,
    ERROR_ENTERING_OFFLINE_ENTITLEMENTS_MODE,
    GOOGLE_PURCHASE_STARTED,
    GOOGLE_PURCHASES_UPDATE_RECEIVED,
    GET_OFFERINGS_STARTED,
    GET_OFFERINGS_RESULT,
    GET_PRODUCTS_STARTED,
    GET_PRODUCTS_RESULT,
    GET_CUSTOMER_INFO_STARTED,
    GET_CUSTOMER_INFO_RESULT,
}
