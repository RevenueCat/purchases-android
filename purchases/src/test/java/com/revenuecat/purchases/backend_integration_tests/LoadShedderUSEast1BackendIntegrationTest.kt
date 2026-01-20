package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import java.net.URL

internal open class LoadShedderUSEast1BackendIntegrationTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.loadShedderApiKey

    @Before
    open fun setup() {
        every { appConfig.baseURL } returns URL("https://fortress-us-east-1.revenuecat.com/")
    }

    @Test
    fun `can perform product entitlement mapping backend request`() {
        var error: PurchasesError? = null
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings).containsOnlyKeys(
                        "com.revenuecat.loadshedder.monthly",
                        "com.revenuecat.loadshedder.monthly:monthly",
                        "consumable.10_coins",
                        "lifetime",
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
                    assertThat(productEntitlementMapping.mappings["consumable.10_coins"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "consumable.10_coins",
                            basePlanId = null,
                            entitlements = emptyList()
                        )
                    )
                    assertThat(productEntitlementMapping.mappings["lifetime"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "lifetime",
                            basePlanId = null,
                            entitlements = listOf("premium", "pro")
                        )
                    )
                    assertThat(productEntitlementMapping.originalSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
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
    }

    @Test
    fun `can perform verified product entitlement mapping backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
        ensureBlockFinishes { latch ->
            backend.getProductEntitlementMapping(
                onSuccessHandler = { productEntitlementMapping ->
                    assertThat(productEntitlementMapping.mappings).containsOnlyKeys(
                        "com.revenuecat.loadshedder.monthly",
                        "com.revenuecat.loadshedder.monthly:monthly",
                        "consumable.10_coins",
                        "lifetime",
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
                    assertThat(productEntitlementMapping.mappings["consumable.10_coins"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "consumable.10_coins",
                            basePlanId = null,
                            entitlements = emptyList()
                        )
                    )
                    assertThat(productEntitlementMapping.mappings["lifetime"]).isEqualTo(
                        ProductEntitlementMapping.Mapping(
                            productIdentifier = "lifetime",
                            basePlanId = null,
                            entitlements = listOf("premium", "pro")
                        )
                    )
                    assertThat(productEntitlementMapping.originalSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
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
                    assertThat(offeringsResponse.getString("current_offering_id")).isEqualTo("default")
                    assertThat(originalDataSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
                    latch.countDown()
                },
                onError = { _, _ ->
                    fail("Expected success")
                }
            )
        }
        val urlString = URL(appConfig.baseURL, Endpoint.GetOfferings("test-user-id").getPath()).toString()
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString(urlString, any())
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
                    assertThat(offeringsResponse.getString("current_offering_id")).isEqualTo("default")
                    assertThat(originalDataSource).isEqualTo(HTTPResponseOriginalSource.LOAD_SHEDDER)
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
