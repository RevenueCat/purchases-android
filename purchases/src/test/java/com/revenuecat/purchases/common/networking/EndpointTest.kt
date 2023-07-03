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
    fun `supportsSignatureValidation returns true for expected values`() {
        val expectedSupportsValidationEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
            Endpoint.GetOfferings("test-user-id"),
            Endpoint.GetProductEntitlementMapping,
        )
        for (endpoint in expectedSupportsValidationEndpoints) {
            assertThat(endpoint.supportsSignatureValidation).isTrue
        }
    }

    @Test
    fun `supportsSignatureValidation returns false for expected values`() {
        val expectedNotSupportsValidationEndpoints = listOf(
            Endpoint.GetAmazonReceipt("test-user-id", "test-receipt-id"),
            Endpoint.PostAttributes("test-user-id"),
            Endpoint.PostDiagnostics,
        )
        for (endpoint in expectedNotSupportsValidationEndpoints) {
            assertThat(endpoint.supportsSignatureValidation).isFalse
        }
    }

    @Test
    fun `verify needsNonceToPerformSigning is true only if supportsSignatureValidation is true`() {
        for (endpoint in allEndpoints) {
            if (!endpoint.supportsSignatureValidation) {
                assertThat(endpoint.needsNonceToPerformSigning).isFalse
            }
        }
    }

    @Test
    fun `needsNonceToPerformSigning is true for expected values`() {
        val expectedEndpoints = listOf(
            Endpoint.GetCustomerInfo("test-user-id"),
            Endpoint.LogIn,
            Endpoint.PostReceipt,
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning).isTrue
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
        )
        for (endpoint in expectedEndpoints) {
            assertThat(endpoint.needsNonceToPerformSigning).isFalse
        }
    }
}
