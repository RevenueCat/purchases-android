package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

internal class ProductionBackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    @Test
    fun `can perform product entitlement mapping backend request`() {
        var error: PurchasesError? = null
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings.size).isEqualTo(2)
                    assertThat(productEntitlementMapping.mappings["cheapest_subs"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "cheapest_subs",
                            basePlanId = "annual",
                            entitlements = listOf("pro_cat")
                        )
                    )
                    latch.countDown()
                },
                onErrorHandler = {
                    error = it
                    latch.countDown()
                }
            )
        }
        assertThat(error).isNull()
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.GetProductEntitlementMapping.getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        verify(exactly = 0) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any())  }
    }

    @Test
    fun `can perform verified product entitlement mapping backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings.size).isEqualTo(36)
                    assertThat(productEntitlementMapping.mappings["annual_freetrial"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "annual_freetrial",
                            basePlanId = "p1y",
                            entitlements = listOf("pro_cat")
                        )
                    )
                    latch.countDown()
                },
                onErrorHandler = {
                    fail("Expected success but got error: $it")
                }
            )
        }
        verify(exactly = 1) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any())  }
    }

    @Test
    fun `can perform offerings backend request`() {
        ensureBlockFinishes { latch ->
            backend.getOfferings(
                appUserID = "test-user-id",
                appInBackground = false,
                onSuccess = { offeringsResponse ->
                    assertThat(offeringsResponse.length()).isPositive
                    latch.countDown()
                },
                onError = { _, _ ->
                    fail("Expected success")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.GetOfferings("test-user-id").getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        verify(exactly = 0) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any())  }
    }

    @Test
    fun `can perform verified offerings backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
        ensureBlockFinishes { latch ->
            backend.getOfferings(
                appUserID = "test-user-id",
                appInBackground = false,
                onSuccess = { offeringsResponse ->
                    assertThat(offeringsResponse.length()).isPositive
                    latch.countDown()
                },
                onError = { error, _ ->
                    fail("Expected success. Got error: $error")
                }
            )
        }
        verify(exactly = 1) { signingManager.verifyResponse(any(), any(), any(), any(), any(), any())  }
    }
}
