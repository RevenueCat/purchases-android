package com.revenuecat.purchases.strings

internal object BillingStrings {
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
    const val CANNOT_CALL_CAN_MAKE_PAYMENTS = "canMakePayments requires the Google Play Store. Skipping " +
        "any checks and returning true."
    const val NULL_ACTIVITY_INTENT = "Activity passed into launchBillingFlow has a null intent, which may cause " +
        "a crash. See https://github.com/RevenueCat/purchases-android/issues/381 for more information."
    const val BILLING_CLIENT_RETRY = "Retrying BillingClient connection after backoff of %s milliseconds."
    const val ILLEGAL_STATE_EXCEPTION_WHEN_CONNECTING = "There was an IllegalStateException when connecting to " +
        "BillingClient. This has been reported to occur on Samsung devices on unknown circumstances.\nException: %s"
    const val BILLING_CONNECTION_ERROR_INAPP_MESSAGES = "Error connecting to billing client to display " +
        "inapp messages: %s"
    const val BILLING_INAPP_MESSAGE_NONE = "No Google Play in-app message was available."
    const val BILLING_INAPP_MESSAGE_UPDATE = "Subscription status was updated from In-App Message."
    const val BILLING_INAPP_MESSAGE_UNEXPECTED_CODE = "Unexpected billing code: %s"
}
