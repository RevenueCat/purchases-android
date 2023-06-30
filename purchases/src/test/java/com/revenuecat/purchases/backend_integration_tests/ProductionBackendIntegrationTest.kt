package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ProductionBackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    @Test
    fun `can perform product entitlement mapping backend request`() {
        var error: PurchasesError? = null
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
    }
}
