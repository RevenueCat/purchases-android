package com.revenuecat.purchases.models

import java.util.Date

data class PurchasedProduct(
    val productIdentifier: String,
    val storeTransaction: StoreTransaction,
    val isActive: Boolean,
    val entitlements: List<String>?,
    val expiresDate: Date?
)
