package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LoadShedderBackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.loadShedderApiKey

    @Test
    fun canPerformProductEntitlementMappingBackendRequest() {
        var error: PurchasesError? = null
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings).containsOnlyKeys(
                        "com.revenuecat.loadshedder.monthly",
                        "com.revenuecat.loadshedder.monthly:monthly"
                    )
                    assertThat(productEntitlementMapping.mappings["com.revenuecat.loadshedder.monthly"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "com.revenuecat.loadshedder.monthly",
                            basePlanId = "monthly",
                            entitlements = listOf("premium", "pro")
                        )
                    )
                    assertThat(productEntitlementMapping.mappings["com.revenuecat.loadshedder.monthly:monthly"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "com.revenuecat.loadshedder.monthly",
                            basePlanId = "monthly",
                            entitlements = listOf("premium", "pro")
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
