package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class EndpointTest {

    private val allEndpoints = listOf(
        Endpoint.GetCustomerInfo("test-user-id"),
        Endpoint.LogIn,
        Endpoint.PostReceipt,
        Endpoint.GetOfferings("test-user-id"),
        Endpoint.GetProductEntitlementMapping,
        Endpoint.GetAmazonReceipt("test-user-id", "test-receipt-id"),
        Endpoint.PostAttributes("test-user-id"),
        Endpoint.PostDiagnostics,
        Endpoint.PostEvents,
        Endpoint.PostRedeemWebPurchase,
        Endpoint.GetVirtualCurrencies("test-user-id"),
        Endpoint.AliasUsers("test-user-id")
    )

    @Test
    fun `GetCustomerInfo has correct path`() {
        val endpoint = Endpoint.GetCustomerInfo("test user-id")
        val expectedPath = "/v1/subscribers/test%20user-id"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostReceipt has correct path`() {
        val endpoint = Endpoint.PostReceipt
        val expectedPath = "/v1/receipts"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetOfferings has correct path`() {
        val endpoint = Endpoint.GetOfferings("test user-id")
        val expectedPath = "/v1/subscribers/test%20user-id/offerings"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `LogIn has correct path`() {
        val endpoint = Endpoint.LogIn
        val expectedPath = "/v1/subscribers/identify"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `Diagnostics has correct path`() {
        val endpoint = Endpoint.PostDiagnostics
        val expectedPath = "/v1/diagnostics"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `Paywall events has correct path`() {
        val endpoint = Endpoint.PostEvents
        val expectedPath = "/v1/events"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostAttributes has correct path`() {
        val endpoint = Endpoint.PostAttributes("test user-id")
        val expectedPath = "/v1/subscribers/test%20user-id/attributes"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetAmazonReceipt has correct path`() {
        val endpoint = Endpoint.GetAmazonReceipt("test user-id", "test-receipt-id")
        val expectedPath = "/v1/receipts/amazon/test%20user-id/test-receipt-id"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetProductEntitlementMapping has correct path`() {
        val endpoint = Endpoint.GetProductEntitlementMapping
        val expectedPath = "/v1/product_entitlement_mapping"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostRedeemWebPurchase has correct path`() {
        val endpoint = Endpoint.PostRedeemWebPurchase
        val expectedPath = "/v1/subscribers/redeem_purchase"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetVirtualCurrencies has correct path`() {
        val endpoint = Endpoint.GetVirtualCurrencies(userId = "test user-id")
        val expectedPath = "/v1/subscribers/test%20user-id/virtual_currencies"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetVirtualCurrencies has correct name`() {
        val endpoint = Endpoint.GetVirtualCurrencies(userId = "test user-id")
        val expectedName = "get_virtual_currencies"
        assertThat(endpoint.name).isEqualTo(expectedName)
    }

    @Test
    fun `WebBillingGetProducts has correct path`() {
        val endpoint = Endpoint.WebBillingGetProducts(userId = "test user-id", linkedSetOf("product1", "product2"))
        val expectedPath = "/rcbilling/v1/subscribers/test%20user-id/products?id=product1&id=product2"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `AliasUsers has correct path`() {
        val endpoint = Endpoint.AliasUsers(userId = "test user-id")
        val expectedPath = "/v1/subscribers/test%20user-id/alias"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `AliasUsers has correct name`() {
        val endpoint = Endpoint.AliasUsers(userId = "test user-id")
        assertThat(endpoint.name).isEqualTo("alias_users")
    }

    @Test
    fun `supportsSignatureVerification returns true for expected values`() {
        val expectedSupportsValidationEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
            Endpoint.GetOfferings("test-user-id"),
            Endpoint.GetProductEntitlementMapping,
            Endpoint.PostRedeemWebPurchase,
            Endpoint.GetVirtualCurrencies(userId = "test-user-id"),
        )
        for (endpoint in expectedSupportsValidationEndpoints) {
            assertThat(endpoint.supportsSignatureVerification)
                .withFailMessage { "Endpoint $endpoint expected to support signature validation" }
                .isTrue
        }
    }

    @Test
    fun `supportsSignatureVerification returns false for expected values`() {
        val expectedNotSupportsValidationEndpoints = listOf(
            Endpoint.GetAmazonReceipt("test-user-id", "test-receipt-id"),
            Endpoint.PostAttributes("test-user-id"),
            Endpoint.PostDiagnostics,
            Endpoint.PostEvents,
            Endpoint.WebBillingGetProducts("test-user-id", setOf("product1", "product2")),
            Endpoint.AliasUsers("test-user-id"),
        )
        for (endpoint in expectedNotSupportsValidationEndpoints) {
            assertThat(endpoint.supportsSignatureVerification)
                .withFailMessage { "Endpoint $endpoint expected to not support signature validation" }
                .isFalse
        }
    }

    @Test
    fun `verify needsNonceToPerformSigning is true only if supportsSignatureVerification is true`() {
        for (endpoint in allEndpoints) {
            if (!endpoint.supportsSignatureVerification) {
                assertThat(endpoint.needsNonceToPerformSigning)
                    .withFailMessage { "Endpoint $endpoint requires nonce but does not support signature validation" }
                    .isFalse
            }
        }
    }

    @Test
    fun `needsNonceToPerformSigning is true for expected values`() {
        val expectedEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
            Endpoint.PostRedeemWebPurchase,
            Endpoint.GetVirtualCurrencies(userId = "test-user-id"),
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning)
                .withFailMessage { "Endpoint $endpoint expected to require nonce for signing" }
                .isTrue
        }
    }

    @Test
    fun `needsNonceToPerformSigning is false for expected values`() {
        val expectedEndpoints = listOf(
            Endpoint.GetOfferings("test-user-id"),
            Endpoint.GetProductEntitlementMapping,
            Endpoint.GetAmazonReceipt("test-user-id", "test-receipt-id"),
            Endpoint.PostAttributes("test-user-id"),
            Endpoint.PostDiagnostics,
            Endpoint.PostEvents,
            Endpoint.WebBillingGetProducts("test-user-id", setOf("product1", "product2")),
            Endpoint.AliasUsers("test-user-id"),
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning)
                .withFailMessage { "Endpoint $endpoint expected to not require nonce for signing" }
                .isFalse
        }
    }
}
