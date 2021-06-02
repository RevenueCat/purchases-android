package com.revenuecat.purchases.strings

object BillingStrings {
    const val BILLING_CLIENT_DISCONNECTED = "Billing is disconnected and purchase methods won't work. Stacktrace: %s"
    const val BILLING_CLIENT_ENDING = "Ending connection for %s"
    const val BILLING_CLIENT_ERROR = "Billing Service Setup finished with error code: %s"
    const val BILLING_CLIENT_STARTING = "Starting connection for %s"
    const val BILLING_INTENT_FAILED = "Failed to launch billing intent. %s"
    const val BILLING_SERVICE_DISCONNECTED = "Billing Service disconnected for %s"
    const val BILLING_SERVICE_SETUP_FINISHED = "Billing Service Setup finished for %s"
    const val BILLING_UNAVAILABLE = "Billing is not available in this device. %s"
    const val BILLING_WRAPPER_PURCHASES_ERROR = "BillingWrapper purchases failed to update: %s"
    const val BILLING_WRAPPER_PURCHASES_UPDATED = "BillingWrapper purchases updated: %s"
    const val BILLING_PURCHASE_HISTORY_RECORD_MORE_THAN_ONE_SKU = "There's more than one sku in the " +
        "PurchaseHistoryRecord, but only one will be used."
    const val BILLING_PURCHASE_MORE_THAN_ONE_SKU = "There's more than one sku in the PurchaseHistoryRecord, " +
        "but only one will be used."
}
