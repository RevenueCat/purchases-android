package com.revenuecat.purchases.common.offlineentitlements

import org.json.JSONObject

data class ProductsEntitlement(
    val products: List<Product>
) {
    companion object {
        fun fromJson(json: JSONObject): ProductsEntitlement {
            val productsArray = json.getJSONArray("products")
            val products = mutableListOf<Product>()
            for (productIndex in 0 until productsArray.length()) {
                val productObject = productsArray.getJSONObject(productIndex)
                val productIdentifier = productObject.getString("id")
                val entitlementsArray = productObject.getJSONArray("entitlements")
                val entitlements = mutableListOf<String>()
                for (entitlementIndex in 0 until entitlementsArray.length()) {
                    entitlements.add(entitlementsArray.getString(entitlementIndex))
                }
                products.add(Product(productIdentifier, entitlements))
            }
            return ProductsEntitlement(products)
        }
    }

    data class Product(
        val identifier: String,
        val entitlements: List<String>
    )
}
