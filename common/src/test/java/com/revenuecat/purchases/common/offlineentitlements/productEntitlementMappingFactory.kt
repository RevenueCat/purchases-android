package com.revenuecat.purchases.common.offlineentitlements

fun createProductEntitlementMapping(
    mappings: Map<String, List<String>> = mapOf(
        "com.revenuecat.foo_1" to listOf("pro_1"),
        "com.revenuecat.foo_2" to listOf("pro_1", "pro_2"),
        "com.revenuecat.foo_3" to listOf("pro_2")
    )
) = ProductEntitlementMappings(
    mappings.map { (productId, entitlements) -> ProductEntitlementMappings.Mapping(productId, entitlements) }
)
