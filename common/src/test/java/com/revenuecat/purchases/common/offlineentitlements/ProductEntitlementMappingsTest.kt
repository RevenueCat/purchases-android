package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductEntitlementMappingsTest {

    @Test
    fun `fromJson parses mappings correctly`() {
        val json = JSONObject(
            """
                {
                    "products": [
                        {
                            "id": "com.revenuecat.foo_1",
                            "entitlements": [
                                "pro_1"
                            ]
                        },
                        {
                            "id": "com.revenuecat.foo_2",
                            "entitlements": [
                                "pro_1",
                                "pro_2"
                            ]
                        },
                        {
                            "id": "com.revenuecat.foo_3",
                            "entitlements": [
                                "pro_2"
                            ]
                        }
                    ]
                }
            """.trimIndent()
        )
        val productEntitlementMappings = ProductEntitlementMappings.fromJson(json)
        val expectedMappingEntitlementMapping = ProductEntitlementMappings(
            listOf(
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_1", listOf("pro_1")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(productEntitlementMappings).isEqualTo(expectedMappingEntitlementMapping)
    }

    @Test
    fun `fromJson parses empty mappings correctly`() {
        val json = JSONObject(
            """
                {
                    "products": []
                }
            """.trimIndent()
        )
        val productEntitlementMappings = ProductEntitlementMappings.fromJson(json)
        assertThat(productEntitlementMappings.mappings.size).isEqualTo(0)
    }

    @Test
    fun `equals returns true if same mappings`() {
        val mappingEntitlement1 = ProductEntitlementMappings(
            listOf(
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_1", listOf("pro_1")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        val mappingEntitlement2 = ProductEntitlementMappings(
            listOf(
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_1", listOf("pro_1")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(mappingEntitlement1).isEqualTo(mappingEntitlement2)
    }

    @Test
    fun `equals returns false if any mapping has different entitlements`() {
        val mappingEntitlement1 = ProductEntitlementMappings(
            listOf(
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_1", listOf("pro_1")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        val mappingEntitlement2 = ProductEntitlementMappings(
            listOf(
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_1", listOf("pro_1")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_2", listOf("pro_1", "pro_3")),
                ProductEntitlementMappings.Mapping("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(mappingEntitlement1).isNotEqualTo(mappingEntitlement2)
    }
}
