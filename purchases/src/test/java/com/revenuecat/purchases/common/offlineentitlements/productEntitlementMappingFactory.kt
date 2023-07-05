package com.revenuecat.purchases.common.offlineentitlements

internal fun createProductEntitlementMapping(
    mappings: Map<String, ProductEntitlementMapping.Mapping> = mapOf(
        "com.revenuecat.foo_1:p1m" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1")),
        "com.revenuecat.foo_1:p1y" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1y", listOf("pro_1", "pro_2")),
        "com.revenuecat.foo_1" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1")),
        "com.revenuecat.foo_2" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_2", null, listOf("pro_3")),
    )
) = ProductEntitlementMapping(mappings)
