package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.models.StoreTransaction
import java.util.Date

internal data class PurchasedProduct(
    public val productIdentifier: String,
    public val basePlanId: String?,
    public val storeTransaction: StoreTransaction,
    public val entitlements: List<String>,
    public val expiresDate: Date?,
)
