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
        Endpoint.PostPaywallEvents,
        Endpoint.PostRedeemWebPurchase("test-user-id"),
    )

    @Test
    fun `GetCustomerInfo has correct path`() {
        val endpoint = Endpoint.GetCustomerInfo("test user-id")
        val expectedPath = "/subscribers/test%20user-id"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostReceipt has correct path`() {
        val endpoint = Endpoint.PostReceipt
        val expectedPath = "/receipts"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetOfferings has correct path`() {
        val endpoint = Endpoint.GetOfferings("test user-id")
        val expectedPath = "/subscribers/test%20user-id/offerings"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `LogIn has correct path`() {
        val endpoint = Endpoint.LogIn
        val expectedPath = "/subscribers/identify"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `Diagnostics has correct path`() {
        val endpoint = Endpoint.PostDiagnostics
        val expectedPath = "/diagnostics"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `Paywall events has correct path`() {
        val endpoint = Endpoint.PostPaywallEvents
        val expectedPath = "/events"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostAttributes has correct path`() {
        val endpoint = Endpoint.PostAttributes("test user-id")
        val expectedPath = "/subscribers/test%20user-id/attributes"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetAmazonReceipt has correct path`() {
        val endpoint = Endpoint.GetAmazonReceipt("test user-id", "test-receipt-id")
        val expectedPath = "/receipts/amazon/test%20user-id/test-receipt-id"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `GetProductEntitlementMapping has correct path`() {
        val endpoint = Endpoint.GetProductEntitlementMapping
        val expectedPath = "/product_entitlement_mapping"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `PostRedeemWebPurchase has correct path`() {
        val endpoint = Endpoint.PostRedeemWebPurchase("test user-id")
        val expectedPath = "/subscribers/test%20user-id/redeem_web_purchase"
        assertThat(endpoint.getPath()).isEqualTo(expectedPath)
    }

    @Test
    fun `supportsSignatureVerification returns true for expected values`() {
        val expectedSupportsValidationEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
            Endpoint.GetOfferings("test-user-id"),
            Endpoint.GetProductEntitlementMapping,
            Endpoint.PostRedeemWebPurchase("test-user-id"),
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
            Endpoint.PostPaywallEvents,
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
            Endpoint.PostRedeemWebPurchase("test-user-id"),
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
            Endpoint.PostPaywallEvents,
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning)
                .withFailMessage { "Endpoint $endpoint expected to not require nonce for signing" }
                .isFalse
        }
    }
}
