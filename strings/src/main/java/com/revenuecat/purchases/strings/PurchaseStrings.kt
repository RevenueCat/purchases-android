package com.revenuecat.purchases.strings

object PurchaseStrings {
    const val ACKNOWLEDGE_PURCHASE = "Acknowledging purchase with token %s"
    const val ACKNOWLEDGE_PURCHASE_ERROR = "Error acknowledging purchase. Will retry next queryPurchases. %s"
    const val BILLING_CLIENT_NOT_CONNECTED = "[QueryPurchases] Skipping updating pending purchase queue since " +
            "BillingClient is not connected yet."
    const val CONSUMING_PURCHASE = "Consuming purchase with token %s"
    const val CONSUMING_PURCHASE_ERROR = "Error consuming purchase. Will retry next queryPurchases. %s"
    const val EXISTING_PURCHASE = "Found existing purchase for SKU: %s"
    const val NO_EXISTING_PURCHASE = "Couldn't find existing purchase for SKU: %s"
    const val OFFERING_STRING = " - offering: "
    const val PRODUCT_CHANGE = "Product change started: %s"
    const val PURCHASE_FINISH_TRANSACTION_FALSE = "finishTransactions is set to false " +
            "and a purchase has been started. Are you sure you want to do this? " +
            "More info here: https://errors.rev.cat/finishTransactions"
    const val PURCHASE_HISTORY_EMPTY = "Purchase history is empty."
    const val PURCHASE_HISTORY_RETRIEVED = "Purchase history retrieved %s"
    const val PURCHASE_PRODUCT = "Purchasing product: %s"
    const val PURCHASE_SYNCED = "Purchase %s synced"
    const val SKU_QUERY = "Querying purchase history for type %s"
    const val STARTING_PURCHASE = "Purchase started - product: %s"
    const val SYNCING_PURCHASE = "Syncing purchases"
    const val SYNCING_PURCHASE_ERROR = "Error syncing purchases %s"
    const val SYNCING_PURCHASE_ERROR_DETAILS = "Error syncing purchases %s. Error: %s"
    const val UPDATE_PURCHASE_QUEUE = "[QueryPurchases] Updating pending purchase queue"
    const val UPGRADE_SKU = "Moving from old SKU %s to sku %s"
    const val UPGRADE_SKU_ERROR = "There was an error trying to upgrade. BillingResponseCode: %s"
}
