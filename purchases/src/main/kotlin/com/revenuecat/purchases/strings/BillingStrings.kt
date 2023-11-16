package com.revenuecat.purchases.strings

internal object BillingStrings {
    const val BILLING_CLIENT_DISCONNECTED = "Billing is disconnected and purchase methods won't work. Stacktrace: %s"
    const val BILLING_CLIENT_ENDING = "Ending connection for %s"
    const val BILLING_CLIENT_ERROR = "Billing Service Setup finished with error code: %s"
    const val BILLING_CLIENT_STARTING = "Starting connection for %s"
    const val BILLING_INTENT_FAILED = "Failed to launch billing intent. %s"
    const val BILLING_SERVICE_DISCONNECTED = "Billing Service disconnected"
    const val BILLING_SERVICE_DISCONNECTED_INSTANCE = "Billing Service disconnected for %s"
    const val BILLING_SERVICE_SETUP_FINISHED = "Billing Service Setup finished for %s"
    const val BILLING_UNAVAILABLE = "Billing is not available in this device. %s"
    const val BILLING_UNAVAILABLE_LESS_THAN_3 = "Billing is not available in this device. Make sure there's an " +
        "account configured in Play Store. Reopen the Play Store or clean its caches if this keeps happening. " +
        "Original error message: %s"
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
        "in-app messages: %s"
    const val BILLING_CONNECTION_ERROR_STORE_COUNTRY = "Error connecting to billing client to get store " +
        "country: %s"
    const val BILLING_AMAZON_ERROR_STORE_COUNTRY = "Error obtaining store country in Amazon: %s"
    const val BILLING_CONFIG_NULL_ON_SUCCESS = "Billing config is null even though Google return OK result"
    const val BILLING_INAPP_MESSAGE_NONE = "No Google Play in-app message was available."
    const val BILLING_INAPP_MESSAGE_UPDATE = "Subscription status was updated from in-app message."
    const val BILLING_INAPP_MESSAGE_UNEXPECTED_CODE = "Unexpected billing code: %s"
    const val BILLING_UNSPECIFIED_INAPP_MESSAGE_TYPES = "Tried to show in-app messages without specifying any types. " +
        "Please add what types of in-app message you want to display."
    const val BILLING_CLIENT_RETRY_ALREADY_SCHEDULED = "BillingClient connection retry already scheduled. Ignoring"
    const val BILLING_INITIATE_GETTING_COUNTRY_CODE = "Billing client: Initiating getting country code."
    const val BILLING_COUNTRY_CODE = "Billing connected with country code: %s"
}
