package com.revenuecat.purchases.google

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct

class GoogleOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? {
        val storeProducts: List<StoreProduct>? = productsById[productIdentifier]
        if (planIdentifier == null) {
            // It could be an INAPP or a mis-configured subscription
            // Try to find INAPP, otherwise null
            return storeProducts
                .takeIf { it?.size == 1 }
                ?.takeIf { it[0].type == ProductType.INAPP }
                ?.firstOrNull()
        }
        return storeProducts?.firstOrNull { storeProduct ->
            storeProduct.subscriptionOptions.firstOrNull { it.isBasePlan }?.id == planIdentifier
        }
    }
}
