package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.models.Transaction
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class TransactionAPI {
    fun check(transaction: Transaction) {
        val revenuecatId: String = transaction.revenuecatId
        val productId: String = transaction.productId
        val purchaseDate: Date = transaction.purchaseDate
    }
}
