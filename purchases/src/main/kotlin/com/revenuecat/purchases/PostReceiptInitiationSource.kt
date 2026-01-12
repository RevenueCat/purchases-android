package com.revenuecat.purchases

@InternalRevenueCatStoreAPI
enum class PostReceiptInitiationSource {
    RESTORE,
    PURCHASE,
    UNSYNCED_ACTIVE_PURCHASES,
    ;

    val postReceiptFieldValue: String
        get() = when (this) {
            RESTORE -> "restore"
            PURCHASE -> "purchase"
            UNSYNCED_ACTIVE_PURCHASES -> "unsynced_active_purchases"
        }
}
