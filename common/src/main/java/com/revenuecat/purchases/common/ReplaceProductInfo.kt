package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.StoreTransaction

data class ReplaceProductInfo(
    val oldPurchase: StoreTransaction,
    val prorationMode: GoogleProrationMode? = null
)
