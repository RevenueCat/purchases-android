package com.revenuecat.purchases.common

import com.revenuecat.purchases.models.StoreProduct
import org.json.JSONObject

class GoogleOfferingParser : OfferingParser() {
    override fun findMatchingProduct(
        productsById: Map<String, List<StoreProduct>>,
        packageJson: JSONObject,
    ): StoreProduct? {
        val productIdentifier = packageJson.getString("platform_product_identifier")

        val planIdentifier = packageJson.optString("platform_product_plan_identifier").takeIf { it.isNotEmpty() }
        val storeProducts: List<StoreProduct>? = productsById[productIdentifier]
        if (planIdentifier == null) {
            // It could be an INAPP or a mis-configured subscription
            // Try to find INAPP, otherwise null
            return storeProducts
                .takeIf { it?.size == 1 }
//                ?.takeIf { it[0].type == ProductType.INAPP }
                ?.firstOrNull()
        }

        return storeProducts?.firstOrNull { storeProduct ->
            storeProduct.subscriptionOptions?.basePlan?.id == planIdentifier
        }
    }
}
