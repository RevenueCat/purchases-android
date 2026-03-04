package com.revenuecat.purchases.strings

import com.revenuecat.purchases.InternalRevenueCatAPI

@InternalRevenueCatAPI
public object PurchaseStrings {
    public const val ACKNOWLEDGING_PURCHASE: String = "Acknowledging purchase with token %s"
    public const val ACKNOWLEDGING_PURCHASE_ERROR: String = "Error acknowledging purchase. Will retry next " +
        "queryPurchases. %s"
    public const val ACKNOWLEDGING_PURCHASE_ERROR_RESTORE: String = "Couldn't acknowledge purchase after restoring " +
        "it, this most likely means the subscription has expired already or the product has already been acknowledged."
    public const val BILLING_CLIENT_NOT_CONNECTED: String = "Skipping updating pending purchase queue since " +
        "BillingClient is not connected yet."
    public const val CONSUMING_PURCHASE: String = "Consuming purchase with token %s"
    public const val CONSUMING_PURCHASE_ERROR: String = "Error consuming purchase. Will retry next queryPurchases. %s"
    public const val CONSUMING_PURCHASE_ERROR_RESTORE: String = "Couldn't consume purchase after restoring it, this " +
        "most likely means the product has already been consumed."
    public const val NOT_CONSUMING_IN_APP_PURCHASE_ACCORDING_TO_BACKEND: String = "Not consuming in-app purchase " +
        "according to server configuration. This is expected for non-consumable products. The user won't be able " +
        "to purchase this product again."
    public const val FOUND_EXISTING_PURCHASE: String = "Found existing purchase for SKU: %s"
    public const val NO_EXISTING_PURCHASE: String = "Couldn't find existing purchase for SKU: %s"
    public const val ERROR_FINDING_PURCHASE: String = "Error finding existing purchase for SKU: %s"
    public const val OFFERING: String = " - offering: "
    public const val PRODUCT_CHANGE_STARTED: String = "Product change started: %s"
    public const val PURCHASE_FINISH_TRANSACTION_FALSE: String = "finishTransactions is set to false " +
        "and a purchase has been started. Are you sure you want to do this? " +
        "More info here: https://errors.rev.cat/finishTransactions"
    public const val PURCHASE_SYNCED: String = "Purchase %s synced"
    public const val PURCHASE_SYNCED_USER_ID: String = "Purchase %s synced %s"
    public const val PURCHASING_PRODUCT: String = "Purchasing product: %s"
    public const val PURCHASE_STARTED: String = "Purchase started - product: %s"
    public const val SYNCING_PURCHASES: String = "Syncing purchases"
    public const val SYNCING_PURCHASE_STORE_USER_ID: String = "Syncing purchase with token %s, for store user ID %s"
    public const val SYNCING_PURCHASES_ERROR: String = "Error syncing purchases. Error: %s"
    public const val SYNCING_PURCHASES_ERROR_DETAILS: String = "Error syncing purchases %s. Error: %s"
    public const val SYNCING_PURCHASE_ERROR_DETAILS_USER_ID: String = "Error syncing purchase %s for store user ID " +
        "%s. Error: %s"
    public const val SYNCED_PURCHASES_SUCCESSFULLY: String = "Synced purchases successfully"
    public const val SYNCING_PURCHASE_SKIPPING: String = "Skipping syncing purchase %s for store user ID %s. " +
        "It has already been posted"
    public const val UPDATING_PENDING_PURCHASE_QUEUE: String = "Updating pending purchase queue"
    public const val NO_PENDING_PURCHASES_TO_SYNC: String = "No pending purchases to sync"
    public const val UPGRADING_SKU: String = "Moving from old SKU %s to sku %s"
    public const val UPGRADING_INVALID_TYPE: String = "UpgradeInfo passed to purchase for non-sub product type. " +
        "No upgrade will occur. Consider using non-upgrade purchase flows for this product."
    public const val UPGRADING_SKU_ERROR: String = "There was an error trying to upgrade. BillingResponseCode: %s"
    public const val NOT_RECOGNIZED_PRODUCT_TYPE: String = "Type of product not recognized."
    public const val SKIPPING_AUTOMATIC_SYNC: String = "Skipping automatic synchronization."
    public const val INVALID_PRODUCT_TYPE: String = "Invalid product type passed to %s."
    public const val INVALID_PURCHASE_TYPE: String = "Purchase for a %s purchase must be a %s."
    public const val INVALID_STORE_PRODUCT_TYPE: String = "StoreProduct for a %s purchase must be a %s."
    public const val INVALID_PRODUCT_NO_PRICE: String = "Error finding a price for %s."
    public const val INVALID_CALLBACK_TYPE_INTERNAL_ERROR: String = "Internal SDK error -- invalid callback type " +
        "passed to startProductChange."
    public const val ENTITLEMENT_EXPIRED_OUTSIDE_GRACE_PERIOD: String = "Entitlement %s is no longer active " +
        "(expired %s) and it's outside grace period window (last updated %s)"
    public const val EXTRA_CONNECTION_CANMAKEPAYMENTS: String = "BillingClient has returned more than once when " +
        "checking if device can make payments with result: %s."
    public const val EXTRA_CALLBACK_CANMAKEPAYMENTS: String = "Callback has already been called when checking if " +
        "device can make payments."
    public const val EXCEPTION_CANMAKEPAYMENTS: String = "Exception received when checking if device can make " +
        "payments: \n%s."
    public const val EMPTY_ADD_ONS_LIST_PASSED: String = "An empty list of add-ons was provided. Will proceed with " +
        "purchasing the base product."
    public const val PURCHASING_ADD_ONS_ONLY_SUPPORTED_ON_PLAY_STORE: String = "Making a purchase with add-ons is " +
        "only supported on the Play Store."
    public const val DEFERRED_PRODUCT_CHANGE_WITH_BASE_PLAN_ID: String = "Passing a basePlanId in oldProductId (%s) " +
        "during a DEFERRED product change is not recommended. The basePlanId will be stripped automatically."
}
