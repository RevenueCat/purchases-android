package com.revenuecat.purchases.google.history

/**
 * Constants for the AIDL-based billing integration.
 * These match the constants used by the Play Billing Library internally.
 */
internal object BillingConstants {
    const val BILLING_API_VERSION = 7

    // Bundle keys returned by the billing service
    const val RESPONSE_CODE = "RESPONSE_CODE"
    const val INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
    const val INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"
    const val INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"

    // Service action and package
    const val BILLING_SERVICE_ACTION = "com.android.vending.billing.InAppBillingService.BIND"
    const val VENDING_PACKAGE = "com.android.vending"
}
