package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class EndpointTest {

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
        Endpoint.GetRewardVerification("test-user-id", "client-transaction-id"),
        Endpoint.AliasUsers("test-user-id"),
        Endpoint.GetRemoteConfig("app"),
        Endpoint.GetRemoteConfigFallback("app"),
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
    fun `GetRewardVerification has correct path`() {
        val endpoint = Endpoint.GetRewardVerification(
            userId = "test user-id",
            clientTransactionId = "client transaction id",
        )
        val expectedPath = "/v1/subscribers/test%20user-id/ads/reward_verifications/client%20transaction%20id"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetRewardVerification has correct name`() {
        val endpoint = Endpoint.GetRewardVerification(
            userId = "test user-id",
            clientTransactionId = "client transaction id",
        )
        val expectedName = "get_reward_verification"
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
    fun `GetRemoteConfig has correct path`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val expectedPath = "/v1/config/app"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetRemoteConfig encodes the domain in the path`() {
        val endpoint = Endpoint.GetRemoteConfig("my domain")
        val expectedPath = "/v1/config/my%20domain"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetRemoteConfig does not support fallback base URLs`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        assertThat(endpoint.supportsFallbackBaseURLs).isFalse
    }

    @Test
    fun `GetRemoteConfig expects an RC Container Format response`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        assertThat(endpoint.expectsRCFormatResponse).isTrue
    }

    @Test
    fun `GetRemoteConfigFallback encodes the domain in the path`() {
        val endpoint = Endpoint.GetRemoteConfigFallback("my domain")
        val expectedPath = "/v1/config/my%20domain"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetRemoteConfigFallback does not support fallback base URLs`() {
        val endpoint = Endpoint.GetRemoteConfigFallback("app")
        assertThat(endpoint.supportsFallbackBaseURLs).isFalse
    }

    @Test
    fun `GetRemoteConfigFallback does not expect an RC Container Format response`() {
        val endpoint = Endpoint.GetRemoteConfigFallback("app")
        assertThat(endpoint.expectsRCFormatResponse).isFalse
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
            Endpoint.GetRewardVerification("test-user-id", "client-transaction-id"),
            Endpoint.GetRemoteConfig("app"),
            Endpoint.GetRemoteConfigFallback("app"),
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
            Endpoint.GetRewardVerification("test-user-id", "client-transaction-id"),
            Endpoint.GetRemoteConfig("app"),
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
            Endpoint.GetRemoteConfigFallback("app"),
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning)
                .withFailMessage { "Endpoint $endpoint expected to not require nonce for signing" }
                .isFalse
        }
    }

    @Test
    fun `usesAPISources is true for main API endpoints`() {
        val mainApiEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
            Endpoint.GetOfferings("test-user-id"),
            Endpoint.AliasUsers("test-user-id"),
            Endpoint.PostAttributes("test-user-id"),
            Endpoint.GetAmazonReceipt("test-user-id", "test-receipt-id"),
            Endpoint.GetProductEntitlementMapping,
            Endpoint.GetCustomerCenterConfig("test-user-id"),
            Endpoint.GetRemoteConfig("app"),
            Endpoint.PostCreateSupportTicket,
            Endpoint.PostRedeemWebPurchase,
            Endpoint.GetVirtualCurrencies("test-user-id"),
            Endpoint.GetRewardVerification("test-user-id", "client-transaction-id"),
            Endpoint.WebBillingGetProducts("test-user-id", setOf("product1", "product2")),
        )
        for (endpoint in mainApiEndpoints) {
            assertThat(endpoint.usesAPISources)
                .withFailMessage { "Endpoint $endpoint expected to use API sources" }
                .isTrue
        }
    }

    @Test
    fun `usesAPISources is false for endpoints hosted elsewhere`() {
        val nonApiEndpoints = listOf(
            Endpoint.PostDiagnostics,
            Endpoint.PostEvents,
        )
        for (endpoint in nonApiEndpoints) {
            assertThat(endpoint.usesAPISources)
                .withFailMessage { "Endpoint $endpoint expected to not use API sources" }
                .isFalse
        }
    }
}
