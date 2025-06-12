package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.Transaction
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class TransactionAPI {
    fun check(transaction: Transaction) {
        with(transaction) {
            val transactionIdentifier: String = transactionIdentifier
            val revenuecatId: String = revenuecatId
            val productIdentifier: String = productIdentifier
            val productId: String = productId
            val purchaseDate: Date = purchaseDate
            val storeTransactionId: String? = storeTransactionId
            val store: Store = store
            val displayName: String? = displayName
            val isSandbox: Boolean = isSandbox
            val originalPurchaseDate: Date? = originalPurchaseDate
            val price: Price? = price
        }

        // Test deprecated constructor
        @Suppress("DEPRECATION")
        val deprecatedTransaction = Transaction(
            transactionIdentifier = "id",
            revenuecatId = "id",
            productIdentifier = "product",
            productId = "product",
            purchaseDate = Date(),
            storeTransactionId = "store_id",
            store = Store.PLAY_STORE,
        )

        // Test new constructor
        val newTransaction = Transaction(
            transactionIdentifier = "id",
            revenuecatId = "id",
            productIdentifier = "product",
            productId = "product",
            purchaseDate = Date(),
            storeTransactionId = "store_id",
            store = Store.PLAY_STORE,
            displayName = "Display Name",
            isSandbox = false,
            originalPurchaseDate = Date(),
            price = null,
        )
    }
}
