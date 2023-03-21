package com.revenuecat.purchases.common.offlineentitlements

import org.json.JSONObject

data class ProductEntitlementMappings(
    val mappings: List<Mapping>
) {
    companion object {
        fun fromJson(json: JSONObject): ProductEntitlementMappings {
            val productsArray = json.getJSONArray("products")
            val mappings = mutableListOf<Mapping>()
            for (productIndex in 0 until productsArray.length()) {
                val productObject = productsArray.getJSONObject(productIndex)
                val productIdentifier = productObject.getString("id")
                val entitlementsArray = productObject.getJSONArray("entitlements")
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
}
