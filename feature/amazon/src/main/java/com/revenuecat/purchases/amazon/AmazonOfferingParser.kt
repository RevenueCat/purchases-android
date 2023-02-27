package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct

class AmazonOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? = productsById[productIdentifier]?.firstOrNull()
}
