package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductEntitlementTest {

    @Test
    fun `fromJson parses product entitlements correctly`() {
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
        val productsEntitlement = ProductsEntitlement.fromJson(json)
        val expectedProductsEntitlement = ProductsEntitlement(
            listOf(
                ProductsEntitlement.Product("com.revenuecat.foo_1", listOf("pro_1")),
                ProductsEntitlement.Product("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductsEntitlement.Product("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(productsEntitlement).isEqualTo(expectedProductsEntitlement)
    }

    @Test
    fun `fromJson parses empty entitlements correctly`() {
        val json = JSONObject(
            """
                {
                    "products": []
                }
            """.trimIndent()
        )
        val productsEntitlement = ProductsEntitlement.fromJson(json)
        assertThat(productsEntitlement.products.size).isEqualTo(0)
    }

    @Test
    fun `equals returns true if same product entitlements`() {
        val productEntitlement1 = ProductsEntitlement(
            listOf(
                ProductsEntitlement.Product("com.revenuecat.foo_1", listOf("pro_1")),
                ProductsEntitlement.Product("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductsEntitlement.Product("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        val productEntitlement2 = ProductsEntitlement(
            listOf(
                ProductsEntitlement.Product("com.revenuecat.foo_1", listOf("pro_1")),
                ProductsEntitlement.Product("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductsEntitlement.Product("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(productEntitlement1).isEqualTo(productEntitlement2)
    }

    @Test
    fun `equals returns false if different entitlements`() {
        val productEntitlement1 = ProductsEntitlement(
            listOf(
                ProductsEntitlement.Product("com.revenuecat.foo_1", listOf("pro_1")),
                ProductsEntitlement.Product("com.revenuecat.foo_2", listOf("pro_1", "pro_2")),
                ProductsEntitlement.Product("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        val productEntitlement2 = ProductsEntitlement(
            listOf(
                ProductsEntitlement.Product("com.revenuecat.foo_1", listOf("pro_1")),
                ProductsEntitlement.Product("com.revenuecat.foo_2", listOf("pro_1", "pro_3")),
                ProductsEntitlement.Product("com.revenuecat.foo_3", listOf("pro_2")),
            )
        )
        assertThat(productEntitlement1).isNotEqualTo(productEntitlement2)
    }
}
