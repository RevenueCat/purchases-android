package com.revenuecat.purchases.common.offlineentitlements

import org.json.JSONArray
import org.json.JSONObject

data class ProductEntitlementMappings(
    val mappings: List<Mapping>
) {
    companion object {
        private const val PRODUCTS_KEY = "products"
        private const val PRODUCT_ID_KEY = "id"
        private const val ENTITLEMENTS_KEY = "entitlements"

        fun fromJson(json: JSONObject): ProductEntitlementMappings {
            val productsArray = json.getJSONArray(PRODUCTS_KEY)
            val mappings = mutableListOf<Mapping>()
            for (productIndex in 0 until productsArray.length()) {
                val productObject = productsArray.getJSONObject(productIndex)
                val productIdentifier = productObject.getString(PRODUCT_ID_KEY)
                val entitlementsArray = productObject.getJSONArray(ENTITLEMENTS_KEY)
                val entitlements = mutableListOf<String>()
                for (entitlementIndex in 0 until entitlementsArray.length()) {
                    entitlements.add(entitlementsArray.getString(entitlementIndex))
                }
                mappings.add(Mapping(productIdentifier, entitlements))
            }
            return ProductEntitlementMappings(mappings)
        }
    }

    data class Mapping(
        val identifier: String,
        val entitlements: List<String>
    )

    fun toMap(): Map<String, List<String>> {
        return mappings.associateBy({ it.identifier }, { it.entitlements })
    }

    fun toJson() = JSONObject().apply {
        val mappingsArray = mappings.map {
            JSONObject().apply {
                put(PRODUCT_ID_KEY, it.identifier)
                put(ENTITLEMENTS_KEY, JSONArray(it.entitlements))
            }
        }
        put(PRODUCTS_KEY, JSONArray(mappingsArray))
    }
}
