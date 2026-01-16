package com.revenuecat.purchases.strings

import com.revenuecat.purchases.InternalRevenueCatAPI

@InternalRevenueCatAPI
object RestoreStrings {
    const val PURCHASE_HISTORY_EMPTY = "Purchase history is empty."
    const val PURCHASE_HISTORY_RETRIEVED = "Purchase history retrieved %s"
    const val EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE = "BillingClient queryPurchaseHistory has returned more than " +
        "once, with result: %s. More info here: https://rev.cat/google-duplicated-listener-timeouts"
    const val PURCHASE_RESTORED = "Purchase %s restored"
    const val QUERYING_PURCHASE = "Querying purchases"
    const val QUERYING_PURCHASE_WITH_HASH = "Purchase of type %s with hash %s"
    const val QUERYING_PURCHASE_WITH_TYPE = "Querying Purchase with %s and type %s"
    const val RESTORING_PURCHASE = "Restoring purchases"
    const val RESTORING_PURCHASE_ERROR = "Error restoring purchase: %s. Error: %s"
    const val RESTORE_PURCHASES_NO_PURCHASES_FOUND = "No purchases found to restore. " +
        "This will happen if the user does not have any active subscriptions or unconsumed one-time products." +
        "Please make sure you're using the correct Store account (Google/Amazon) and that you have configured your " +
        "one-time products correctly as either consumables (that can be purchased multiple times) or non-consumables " +
        "(that can only be purchased one by each user) in the RevenueCat dashboard. " +
        "This will return the current CustomerInfo."
    const val SHARING_ACC_RESTORE_FALSE = "allowSharingPlayStoreAccount is set to false and restorePurchases " +
        "has been called. This will 'alias' any app user id's sharing the same receipt. " +
        "Are you sure you want to do this? More info here: https://errors.rev.cat/allowsSharingPlayStoreAccount"
    const val RESTORE_PURCHASES_SIMULATED_STORE = "Restoring purchases not available in test store. " +
        "Returning current CustomerInfo."
    const val SYNC_PURCHASES_SIMULATED_STORE = "Syncing purchases not available in test store. " +
        "Returning current CustomerInfo."
    const val QUERYING_PURCHASE_HISTORY = "Querying purchase history for type %s"
    const val QUERYING_SUBS_ERROR = "Error when querying subscriptions. %s"
    const val QUERYING_INAPP_ERROR = "Error when querying inapps. %s"
}
