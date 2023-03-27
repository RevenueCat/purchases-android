package com.revenuecat.purchases.common.offlineentitlements

fun createProductEntitlementMapping(
    mappings: Map<String, List<String>> = mapOf(
        "com.revenuecat.foo_1" to listOf("pro_1"),
        "com.revenuecat.foo_2" to listOf("pro_1", "pro_2"),
        "com.revenuecat.foo_3" to listOf("pro_2")
    )
) = ProductEntitlementMapping(
    mappings.map { (productId, entitlements) -> ProductEntitlementMapping.Mapping(productId, entitlements) }
)
