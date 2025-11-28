package com.revenuecat.purchases.google.history

/**
 * Constants for the AIDL-based billing integration.
 * These match the constants used by the Play Billing Library internally.
 */
internal object BillingConstants {
    const val BILLING_API_VERSION = 7

    // Response codes
    const val BILLING_RESPONSE_RESULT_OK = 0
    const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
    const val BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE = 2
    const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
    const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
    const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
    const val BILLING_RESPONSE_RESULT_ERROR = 6
    const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7
    const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8

    // Bundle keys returned by the billing service
    const val RESPONSE_CODE = "RESPONSE_CODE"
    const val INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
    const val INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"
    const val INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"

    // Item types
    const val ITEM_TYPE_INAPP = "inapp"
    const val ITEM_TYPE_SUBS = "subs"

    // Service action and package
    const val BILLING_SERVICE_ACTION = "com.android.vending.billing.InAppBillingService.BIND"
    const val VENDING_PACKAGE = "com.android.vending"

    /**
     * Converts a response code to a human-readable string.
     */
    fun getResponseCodeString(responseCode: Int): String {
        return when (responseCode) {
            BILLING_RESPONSE_RESULT_OK -> "OK"
            BILLING_RESPONSE_RESULT_USER_CANCELED -> "User Canceled"
            BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE -> "Service Unavailable"
            BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "Billing Unavailable"
            BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "Item Unavailable"
            BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "Developer Error"
            BILLING_RESPONSE_RESULT_ERROR -> "Error"
            BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED -> "Item Already Owned"
            BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED -> "Item Not Owned"
            else -> "Unknown ($responseCode)"
        }
    }
}
