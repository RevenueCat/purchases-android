package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.paywalls.events.PaywallBackendEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventRequest
import com.revenuecat.purchases.paywalls.events.PaywallEventType
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
        assertSigningNotPerformed()
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
        assertSigningPerformed()
    }

    @Test
    fun `can perform login backend request`() {
        ensureBlockFinishes { latch ->
            backend.logIn(
                appUserID = "test-user-id",
                newAppUserID = "new-test-user-id",
                onSuccessHandler = { customerInfo, _ ->
                    assertThat(customerInfo.originalAppUserId).isEqualTo("new-test-user-id")
                    latch.countDown()
                },
                onErrorHandler = {
                    fail("Expected success")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.LogIn.getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        assertSigningNotPerformed()
    }

    @Test
    fun `can perform verified login backend request`() {
        setupTest(SignatureVerificationMode.Enforced())
        ensureBlockFinishes { latch ->
            backend.logIn(
                appUserID = "test-user-id",
                newAppUserID = "new-test-user-id",
                onSuccessHandler = { customerInfo, _ ->
                    assertThat(customerInfo.originalAppUserId).isEqualTo("new-test-user-id")
                    assertThat(customerInfo.entitlements.verification).isEqualTo(VerificationResult.VERIFIED)
                    latch.countDown()
                },
                onErrorHandler = {
                    fail("Expected success")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.LogIn.getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        assertSigningPerformed()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `can postPaywallEvents backend request`() {
        val request = PaywallEventRequest(listOf(
            PaywallBackendEvent(
                id = "id",
                version = 1,
                type = PaywallEventType.CANCEL.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallRevision = 5,
                timestamp = 123456789,
                displayMode = "footer",
                darkMode = true,
                localeIdentifier = "en_US",
            )
        ))
        ensureBlockFinishes { latch ->
            backend.postPaywallEvents(
                request,
                onSuccessHandler = {
                    latch.countDown()
                },
                onErrorHandler = { error, _ ->
                    fail("Expected success. Got $error")
                }
            )
        }
        verify(exactly = 1) {
            // Verify we save the backend response in the shared preferences
            sharedPreferencesEditor.putString("/v1${Endpoint.PostPaywallEvents.getPath()}", any())
        }
        verify(exactly = 1) { sharedPreferencesEditor.apply() }
        assertSigningNotPerformed()
    }
}
