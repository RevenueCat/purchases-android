package com.revenuecat.purchases.common

import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.StoreTransaction

internal data class ReplaceProductInfo(
    val oldPurchase: StoreTransaction,
    val replacementMode: ReplacementMode? = null,
)
