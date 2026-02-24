package com.revenuecat.purchases.strings

import com.revenuecat.purchases.InternalRevenueCatAPI

@InternalRevenueCatAPI
public object RestoreStrings {
    public const val PURCHASE_HISTORY_EMPTY: String = "Purchase history is empty."
    public const val PURCHASE_HISTORY_RETRIEVED: String = "Purchase history retrieved %s"
    public const val EXTRA_QUERY_PURCHASE_HISTORY_RESPONSE: String = "BillingClient queryPurchaseHistory has returned " +
        "more than once, with result: %s. More info here: https://rev.cat/google-duplicated-listener-timeouts"
    public const val PURCHASE_RESTORED: String = "Purchase %s restored"
    public const val QUERYING_PURCHASE: String = "Querying purchases"
    public const val QUERYING_PURCHASE_WITH_HASH: String = "Purchase of type %s with hash %s"
    public const val QUERYING_PURCHASE_WITH_TYPE: String = "Querying Purchase with %s and type %s"
    public const val RESTORING_PURCHASE: String = "Restoring purchases"
    public const val RESTORING_PURCHASE_ERROR: String = "Error restoring purchase: %s. Error: %s"
    public const val RESTORE_PURCHASES_NO_PURCHASES_FOUND: String = "No purchases found to restore. " +
        "This will happen if the user does not have any active subscriptions or unconsumed one-time products." +
        "Please make sure you're using the correct Store account (Google/Amazon) and that you have configured your " +
        "one-time products correctly as either consumables (that can be purchased multiple times) or non-consumables " +
        "(that can only be purchased one by each user) in the RevenueCat dashboard. " +
        "This will return the current CustomerInfo."
    public const val SHARING_ACC_RESTORE_FALSE: String = "allowSharingPlayStoreAccount is set to false and " +
        "restorePurchases has been called. This will 'alias' any app user id's sharing the same receipt. " +
        "Are you sure you want to do this? More info here: https://errors.rev.cat/allowsSharingPlayStoreAccount"
    public const val RESTORE_PURCHASES_SIMULATED_STORE: String = "Restoring purchases not available in test store. " +
        "Returning current CustomerInfo."
    public const val SYNC_PURCHASES_SIMULATED_STORE: String = "Syncing purchases not available in test store. " +
        "Returning current CustomerInfo."
    public const val QUERYING_PURCHASE_HISTORY: String = "Querying purchase history for type %s"
    public const val QUERYING_SUBS_ERROR: String = "Error when querying subscriptions. %s"
    public const val QUERYING_INAPP_ERROR: String = "Error when querying inapps. %s"
}
