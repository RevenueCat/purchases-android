package com.revenuecat.purchases.strings

object PurchaseStrings {
    const val ACKNOWLEDGING_PURCHASE = "Acknowledging purchase with token %s"
    const val ACKNOWLEDGING_PURCHASE_ERROR = "Error acknowledging purchase. Will retry next queryPurchases. %s"
    const val BILLING_CLIENT_NOT_CONNECTED = "Skipping updating pending purchase queue since " +
            "BillingClient is not connected yet."
    const val CONSUMING_PURCHASE = "Consuming purchase with token %s"
    const val CONSUMING_PURCHASE_ERROR = "Error consuming purchase. Will retry next queryPurchases. %s"
    const val FOUND_EXISTING_PURCHASE = "Found existing purchase for SKU: %s"
    const val NO_EXISTING_PURCHASE = "Couldn't find existing purchase for SKU: %s"
    const val ERROR_FINDING_PURCHASE = "Error finding existing purchase for SKU: %s"
    const val OFFERING = " - offering: "
    const val PRODUCT_CHANGE_STARTED = "Product change started: %s"
    const val PURCHASE_FINISH_TRANSACTION_FALSE = "finishTransactions is set to false " +
            "and a purchase has been started. Are you sure you want to do this? " +
            "More info here: https://errors.rev.cat/finishTransactions"
    const val PURCHASE_SYNCED = "Purchase %s synced"
    const val PURCHASE_SYNCED_USER_ID = "Purchase %s synced %s"
    const val PURCHASING_PRODUCT = "Purchasing product: %s"
    const val PURCHASE_STARTED = "Purchase started - product: %s"
    const val SYNCING_PURCHASES = "Syncing purchases"
    const val SYNCING_PURCHASE_STORE_USER_ID = "Syncing purchase with token %s, for store user ID %s"
    const val SYNCING_PURCHASES_ERROR = "Error syncing purchases. Error: %s"
    const val SYNCING_PURCHASES_ERROR_DETAILS = "Error syncing purchases %s. Error: %s"
    const val SYNCING_PURCHASE_ERROR_DETAILS_USER_ID = "Error syncing purchase %s for store user ID %s. Error: %s"
    const val SYNCED_PURCHASES_SUCCESSFULLY = "Synced purchases successfully"
    const val SYNCING_PURCHASE_SKIPPING = "Skipping syncing purchase %s for store user ID %s. " +
        "It has already been posted"
    const val UPDATING_PENDING_PURCHASE_QUEUE = "Updating pending purchase queue"
    const val UPGRADING_SKU = "Moving from old SKU %s to sku %s"
    const val UPGRADING_INVALID_TYPE = "UpgradeInfo passed to purchase for non-sub product type. No upgrade will " +
        "occur. Consider using non-upgrade purchase flows for this product."
    const val UPGRADING_SKU_ERROR = "There was an error trying to upgrade. BillingResponseCode: %s"
    const val NOT_RECOGNIZED_PRODUCT_TYPE = "Type of product not recognized."
    const val SKIPPING_AUTOMATIC_SYNC = "Skipping automatic synchronization."
    const val INVALID_PRODUCT_TYPE = "Invalid product type passed to %s."
    const val INVALID_PURCHASE_TYPE = "Purchase for a %s purchase must be a %s."
    const val INVALID_STORE_PRODUCT_TYPE = "StoreProduct for a %s purchase must be a %s."
    const val INVALID_PRODUCT_NO_PRICE = "Error finding a price for %s."
    const val INVALID_CALLBACK_TYPE_INTERNAL_ERROR = "Internal SDK error -- invalid callback type passed to " +
        "startProductChange."
    const val ENTITLEMENT_EXPIRED_OUTSIDE_GRACE_PERIOD = "Entitlement %s is no longer active (expired %s) " +
        "and it's outside grace period window (last updated %s)"
}
