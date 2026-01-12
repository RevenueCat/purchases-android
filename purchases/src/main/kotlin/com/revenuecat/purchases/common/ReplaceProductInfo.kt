package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatStoreAPI
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.StoreTransaction

@InternalRevenueCatStoreAPI
data class ReplaceProductInfo(
    val oldPurchase: StoreTransaction,
    val replacementMode: ReplacementMode? = null,
)
