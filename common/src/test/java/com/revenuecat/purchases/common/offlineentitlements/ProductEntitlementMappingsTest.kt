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

    private val sampleResponseJson = JSONObject(
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

    @Test
    fun `fromJson parses mappings correctly`() {
        val productEntitlementMappings = ProductEntitlementMappings.fromJson(sampleResponseJson)
        val expectedEntitlementMappings = createProductEntitlementMapping()
        assertThat(productEntitlementMappings).isEqualTo(expectedEntitlementMappings)
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
        val mappings1 = createProductEntitlementMapping()
        val mappings2 = createProductEntitlementMapping()
        assertThat(mappings1).isEqualTo(mappings2)
    }

    @Test
    fun `equals returns false if any mapping has different entitlements`() {
        val mappings1 = createProductEntitlementMapping()
        val mappings2 = createProductEntitlementMapping(
            mapOf(
                "com.revenuecat.foo_1" to listOf("pro_1"),
                "com.revenuecat.foo_2" to listOf("pro_1", "pro_3"),
                "com.revenuecat.foo_3" to listOf("pro_2")
            )
        )
        assertThat(mappings1).isNotEqualTo(mappings2)
    }

    @Test
    fun `toMap transforms mappings to map`() {
        val mappingsMap = createProductEntitlementMapping().toMap()
        val expectedMap = mapOf(
            "com.revenuecat.foo_1" to listOf("pro_1"),
            "com.revenuecat.foo_2" to listOf("pro_1", "pro_2"),
            "com.revenuecat.foo_3" to listOf("pro_2"),
        )
        assertThat(mappingsMap).isEqualTo(expectedMap)
    }

    @Test
    fun `toJson transforms mappings back to original Json`() {
        val mappings = ProductEntitlementMappings.fromJson(sampleResponseJson)
        assertThat(mappings.toJson().toString()).isEqualTo(sampleResponseJson.toString())
    }
}
