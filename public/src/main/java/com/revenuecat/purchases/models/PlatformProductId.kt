package com.revenuecat.purchases.models

open class PlatformProductId(open val productId: String) {
    open val asMap: Map<String, String?>
        get() = mapOf(
            "product_id" to productId
        )

    fun toId() = productId

}