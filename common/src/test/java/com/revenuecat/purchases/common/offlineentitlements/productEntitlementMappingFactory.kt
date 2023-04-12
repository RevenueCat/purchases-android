package com.revenuecat.purchases.common.offlineentitlements

fun createProductEntitlementMapping(
    mappings: Map<String, List<String>> = mapOf(
        "com.revenuecat.foo_1:p1m" to listOf("pro_1"),
        "com.revenuecat.foo_1:not_bw" to listOf("pro_2"),
        "com.revenuecat.foo_1" to listOf("pro_1"),
        "com.revenuecat.foo_2" to listOf("pro_1", "pro_2"),
        "com.revenuecat.foo_3" to listOf("pro_2")
    ),
    basePlans: Map<String, String> = mapOf(
        "com.revenuecat.foo_1:p1m" to "p1m",
        "com.revenuecat.foo_1:not_bw" to "not_bw",
        "com.revenuecat.foo_1" to "p1m",
        "com.revenuecat.foo_2" to "p1m",
        "com.revenuecat.foo_3" to "p1m"
    )
) = ProductEntitlementMapping(
    mappings.map { (productId, entitlements) -> ProductEntitlementMapping.Mapping(productId, entitlements, basePlans[productId]) }
)
