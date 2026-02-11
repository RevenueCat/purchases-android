package com.revenuecat.purchases

import kotlinx.serialization.Serializable

@Serializable
internal enum class PostReceiptInitiationSource {
    RESTORE,
    PURCHASE,
    UNSYNCED_ACTIVE_PURCHASES,
    ;

    public val postReceiptFieldValue: String
        get() = when (this) {
            RESTORE -> "restore"
            PURCHASE -> "purchase"
            UNSYNCED_ACTIVE_PURCHASES -> "unsynced_active_purchases"
        }
}
