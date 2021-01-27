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
    const val OFFERING = " - offering: "
    const val PRODUCT_CHANGE_STARTED = "Product change started: %s"
    const val PURCHASE_FINISH_TRANSACTION_FALSE = "finishTransactions is set to false " +
            "and a purchase has been started. Are you sure you want to do this? " +
            "More info here: https://errors.rev.cat/finishTransactions"
    const val PURCHASE_SYNCED = "Purchase %s synced"
    const val PURCHASING_PRODUCT = "Purchasing product: %s"
    const val PURCHASE_STARTED = "Purchase started - product: %s"
    const val SYNCING_PURCHASES = "Syncing purchases"
    const val SYNCING_PURCHASES_ERROR = "Error syncing purchases %s"
    const val SYNCING_PURCHASES_ERROR_DETAILS = "Error syncing purchases %s. Error: %s"
    const val UPDATING_PENDING_PURCHASE_QUEUE = "Updating pending purchase queue"
    const val UPGRADING_SKU = "Moving from old SKU %s to sku %s"
    const val UPGRADING_SKU_ERROR = "There was an error trying to upgrade. BillingResponseCode: %s"
}
