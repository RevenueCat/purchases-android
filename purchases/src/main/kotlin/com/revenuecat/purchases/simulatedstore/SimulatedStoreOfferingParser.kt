package com.revenuecat.purchases.simulatedstore

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

internal class SimulatedStoreOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        val productIdentifier = packageJson.getString("platform_product_identifier")
        return productsById[productIdentifier]?.first() // For simulated store, only one product per id should exist.
    }
}
