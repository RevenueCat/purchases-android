package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.common.DataSource
import com.revenuecat.purchases.common.OriginalDataSource

internal fun createProductEntitlementMapping(
    mappings: Map<String, ProductEntitlementMapping.Mapping> = mapOf(
        "com.revenuecat.foo_1:p1m" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1")),
        "com.revenuecat.foo_1:p1y" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1y", listOf("pro_1", "pro_2")),
        "com.revenuecat.foo_1" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1")),
        "com.revenuecat.foo_2" to ProductEntitlementMapping.Mapping("com.revenuecat.foo_2", null, listOf("pro_3")),
    ),
    originalSource: OriginalDataSource = OriginalDataSource.MAIN,
    dataSource: DataSource = DataSource.MAIN,
) = ProductEntitlementMapping(mappings, originalSource, dataSource)
