package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.models.StoreTransaction
import java.util.Date

data class PurchasedProduct(
    val productIdentifier: String,
    val basePlanId: String?,
    val storeTransaction: StoreTransaction,
    val entitlements: List<String>,
    val expiresDate: Date?
)
