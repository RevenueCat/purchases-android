package com.revenuecat.purchases

import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct

internal val StoreProduct.bestPurchaseOption: PurchaseOption?
    get() {
        if (purchaseOptions.size == 1) return purchaseOptions[0]
        return purchaseOptions.firstOrNull { it.isBasePlan }
    }
