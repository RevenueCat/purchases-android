package com.revenuecat.purchases.teststore

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

internal class TestStoreOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        val productIdentifier = packageJson.getString("platform_product_identifier")
        return productsById[productIdentifier]?.first() // For test store, only one product per identifier should exist.
    }
}
