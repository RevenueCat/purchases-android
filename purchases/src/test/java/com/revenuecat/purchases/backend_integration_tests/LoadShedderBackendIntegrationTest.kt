package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class LoadShedderBackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.loadShedderApiKey

    @Test
    fun `can perform product entitlement mapping backend request`() {
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
    }
}
