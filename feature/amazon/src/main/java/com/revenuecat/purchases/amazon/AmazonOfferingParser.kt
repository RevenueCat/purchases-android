package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct

class AmazonOfferingParser : OfferingParser() {
    override fun Map<String, List<StoreProduct>>.findMatchingProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? = this[productIdentifier]?.firstOrNull()
}
