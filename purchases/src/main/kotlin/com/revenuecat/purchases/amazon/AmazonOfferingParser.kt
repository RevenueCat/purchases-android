package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

internal class AmazonOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        val productIdentifier = packageJson.getString("platform_product_identifier")
        return productsById[productIdentifier]?.firstOrNull()
    }
}
