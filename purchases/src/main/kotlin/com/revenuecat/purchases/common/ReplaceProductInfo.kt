package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.models.StoreTransaction

@Suppress("ForbiddenPublicDataClass")
@InternalRevenueCatAPI
public data class ReplaceProductInfo(
    val oldPurchase: StoreTransaction,
    val replacementMode: ReplacementMode? = null,
)
