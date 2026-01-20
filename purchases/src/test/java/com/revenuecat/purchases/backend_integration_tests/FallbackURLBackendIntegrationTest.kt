package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

internal class FallbackURLBackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey
    override val forceServerErrorStrategy: ForceServerErrorStrategy? = ForceServerErrorStrategy.failExceptFallbackUrls

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
                    assertThat(productEntitlementMapping.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
                    assertThat(productEntitlementMapping.loadedFromCache).isFalse
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
            sharedPreferencesEditor.putString(
                "https://api-production.8-lives-cat.io/v1/product_entitlement_mapping",
                any(),
            )
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        assertSigningNotPerformed()
    }

    @Test
    fun `can perform verified product entitlement mapping backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
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
                    assertThat(productEntitlementMapping.originalSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
                    assertThat(productEntitlementMapping.loadedFromCache).isFalse
                    latch.countDown()
                },
                onErrorHandler = {
                    fail("Expected success but got error: $it")
                }
            )
        }
        assertSigningPerformed()
    }

    @Test
    fun `can perform offerings backend request`() {
        ensureBlockFinishes { latch ->
            backend.getOfferings(
                appUserID = "test-user-id",
                appInBackground = false,
                onSuccess = { offeringsResponse, originalDataSource ->
                    assertThat(offeringsResponse.getJSONArray("offerings").length()).isGreaterThan(0)
                    assertThat(originalDataSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
                    latch.countDown()
                },
                onError = { purchasesError, _ ->
                    fail("Expected success. Got error: $purchasesError")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString(
                "https://api-production.8-lives-cat.io/v1/offerings",
                any(),
            )
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        assertSigningNotPerformed()
    }

    @Test
    fun `can perform verified offerings backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
        ensureBlockFinishes { latch ->
            backend.getOfferings(
                appUserID = "test-user-id",
                appInBackground = false,
                onSuccess = { offeringsResponse, originalDataSource ->
                    assertThat(offeringsResponse.getJSONArray("offerings").length()).isGreaterThan(0)
                    assertThat(originalDataSource).isEqualTo(HTTPResponseOriginalSource.FALLBACK)
                    latch.countDown()
                },
                onError = { error, _ ->
                    fail("Expected success. Got error: $error")
                }
            )
        }
        assertSigningPerformed()
    }
}
