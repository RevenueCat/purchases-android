package com.revenuecat.purchases.common

import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.StoreTransaction

internal data class ReplaceProductInfo(
    public val oldPurchase: StoreTransaction,
    public val replacementMode: ReplacementMode? = null,
)
