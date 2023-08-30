package com.revenuecat.purchases

internal enum class PostReceiptInitiationSource {
    RESTORE,
    PURCHASE,
    QUEUE,
    ;

    val postReceiptFieldValue: String
        get() = when (this) {
            RESTORE -> "restore"
            PURCHASE -> "purchase"
            QUEUE -> "queue"
        }
}
