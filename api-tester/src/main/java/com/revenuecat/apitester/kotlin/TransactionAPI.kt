package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.Transaction
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class TransactionAPI {
    fun check(transaction: Transaction) {
        with(transaction) {
            val revenuecatId: String = revenuecatId
            val productId: String = productId
            val purchaseDate: Date = purchaseDate
        }
    }
}
