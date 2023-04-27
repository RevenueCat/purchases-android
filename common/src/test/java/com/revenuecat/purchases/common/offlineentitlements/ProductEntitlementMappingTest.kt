package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductEntitlementMappingTest {

    private val sampleResponseJson = JSONObject(
        """
            {
                "product_entitlement_mapping": {
                    "com.revenuecat.foo_1:p1m": {
                        "product_identifier": "com.revenuecat.foo_1",
                        "base_plan_id": "p1m",
                        "entitlements": [
                            "pro_1"
                        ]
                    },
                    "com.revenuecat.foo_1:p1y": {
                        "product_identifier": "com.revenuecat.foo_1",
                        "base_plan_id": "p1y",
                        "entitlements": [
                            "pro_1",
                            "pro_2"
                        ]
                    },
                    "com.revenuecat.foo_1": {
                        "product_identifier": "com.revenuecat.foo_1",
                        "base_plan_id": "p1m",
                        "entitlements": [
                            "pro_1"
                        ]
                    },
                    "com.revenuecat.foo_2": {
                        "product_identifier": "com.revenuecat.foo_2",
                        "entitlements": [
                            "pro_3"
                        ]
                    }
                }
            }
        """.trimIndent()
    )

    @Test
    fun `fromJson parses mappings correctly`() {
        val productEntitlementMapping = ProductEntitlementMapping.fromJson(sampleResponseJson)
        assertThat(productEntitlementMapping.mappings.size).isEqualTo(4)
        assertThat(productEntitlementMapping.mappings).containsKeys(
            "com.revenuecat.foo_1:p1m",
            "com.revenuecat.foo_1:p1y",
            "com.revenuecat.foo_1",
            "com.revenuecat.foo_2"
        )
        assertThat(productEntitlementMapping.mappings["com.revenuecat.foo_1:p1m"]).isEqualTo(
            ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1"))
        )
        assertThat(productEntitlementMapping.mappings["com.revenuecat.foo_1:p1y"]).isEqualTo(
            ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1y", listOf("pro_1", "pro_2"))
        )
        assertThat(productEntitlementMapping.mappings["com.revenuecat.foo_1"]).isEqualTo(
            ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_1"))
        )
        assertThat(productEntitlementMapping.mappings["com.revenuecat.foo_2"]).isEqualTo(
            ProductEntitlementMapping.Mapping("com.revenuecat.foo_2", null, listOf("pro_3"))
        )
        val expectedEntitlementMapping = createProductEntitlementMapping()
        assertThat(productEntitlementMapping).isEqualTo(expectedEntitlementMapping)
    }

    @Test
    fun `fromJson parses empty mappings correctly`() {
        val json = JSONObject(
            """
                {
                    "product_entitlement_mapping": {}
                }
            """.trimIndent()
        )
        val productEntitlementMapping = ProductEntitlementMapping.fromJson(json)
        assertThat(productEntitlementMapping.mappings.size).isEqualTo(0)
    }

    @Test
    fun `equals returns true if same mappings`() {
        val mappings1 = createProductEntitlementMapping()
        val mappings2 = createProductEntitlementMapping()
        assertThat(mappings1).isEqualTo(mappings2)
    }

    @Test
    fun `equals returns false if mapping are different`() {
        val mappings1 = createProductEntitlementMapping()
        val mappings2 = createProductEntitlementMapping(
            mappings1.mappings.toMutableMap().apply {
                put("com.revenuecat.foo_1:p1m",
                    ProductEntitlementMapping.Mapping("com.revenuecat.foo_1", "p1m", listOf("pro_2")))
            }
        )
        assertThat(mappings1).isNotEqualTo(mappings2)
    }

    @Test
    fun `toJson transforms mappings back to original Json`() {
        val mappings = ProductEntitlementMapping.fromJson(sampleResponseJson)
        assertThat(mappings.toJson().toString()).isEqualTo(sampleResponseJson.toString())
    }
}
