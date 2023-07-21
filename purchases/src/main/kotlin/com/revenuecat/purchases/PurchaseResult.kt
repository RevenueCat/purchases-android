package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreTransaction

/**
 * The result of a successful purchase operation. Used in coroutines.
 */
data class PurchaseResult(
    /**
     * The [StoreTransaction] for this purchase.
     */
    val storeTransaction: StoreTransaction,

    /**
     * The updated [CustomerInfo] for this user after the purchase has been synced with RevenueCat's servers.
     */
    val customerInfo: CustomerInfo,
)
