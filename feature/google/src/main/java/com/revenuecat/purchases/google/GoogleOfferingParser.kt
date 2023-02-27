package com.revenuecat.purchases.google

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct

class GoogleOfferingParser : OfferingParser() {
    override fun Map<String, List<StoreProduct>>.findMatchingProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? {
        if (planIdentifier == null) {
            // It could be an INAPP or a mis-configured subscription
            // Try to find INAPP, otherwise null
            return this[productIdentifier]
                .takeIf { it?.size == 1 }
                ?.takeIf { it[0].type == ProductType.INAPP }
                ?.firstOrNull()
        }
        val storeProducts: List<StoreProduct>? = this[productIdentifier]
        return storeProducts?.firstOrNull { storeProduct ->
            storeProduct.subscriptionOptions.firstOrNull { it.isBasePlan }?.id == planIdentifier
        }
    }
}
