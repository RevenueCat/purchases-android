@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.utils.stubStoreTransactionFromGooglePurchase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Scenario 12: with `iamEnabled(false)` the SDK behaves as it did before IAM. */
internal class IAMDisabledEndToEndTest : IAMEndToEndTest() {

    @Test
    fun `the first foreground uses the API key on legacy paths and never calls auth`() {
        configure(iamEnabled = false)
        val user = currentAppUserID

        foreground()
        awaitUntil("the foreground CustomerInfo") { customerInfoUpdates.isNotEmpty() }
        awaitQuiet()

        assertNoIAMTraffic()
        assertThat(backend.requests.map { it.path }).contains(
            "/v1/subscribers/${encoded(user)}",
            "/v1/subscribers/${encoded(user)}/offerings",
        )
        assertThat(delegate.tokens).isEmpty()
        assertThat(purchases.authentication.currentAccessToken).isNull()
    }

    @Test
    fun `configuring makes no requests`() {
        configure(iamEnabled = false)

        awaitQuiet()

        assertThat(backend.requests).isEmpty()
    }

    @Test
    fun `login, purchase, restore and logout use the legacy endpoints`() {
        configure(iamEnabled = false)
        foreground()
        awaitQuiet()
        var loggedIn = false

        purchases.logInWith("custom-id", onError = { loggedIn = true }) { _, _ -> loggedIn = true }
        awaitUntil("the login") { loggedIn }
        billing.deliverPurchase(stubStoreTransactionFromGooglePurchase(listOf("monthly"), PURCHASE_TIME))
        awaitUntil("the receipt post") { backend.requests("/v1/receipts").isNotEmpty() }
        purchases.restorePurchases(noopCallback())
        awaitQuiet()
        awaitCustomerInfo { purchases.logOut(it) }
        awaitQuiet()

        assertNoIAMTraffic()
        assertThat(backend.requests("/v1/subscribers/identify")).hasSize(1)
        assertThat(backend.requests("/v1/receipts").first().body?.getString("app_user_id")).isNotEmpty()
    }

    @Test
    fun `the Authentication API refuses to log in`() {
        // Pins today's ConfigurationError; Step D switches it to UnsupportedError.
        configure(iamEnabled = false)

        val result = awaitCustomerInfo {
            purchases.authentication.logIn(Identity.oidc("oidc-id-token".toByteArray()), it)
        }
        awaitQuiet()

        assertThat(result.purchasesError?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertNoIAMTraffic()
    }

    @Test
    fun `getCustomerInfo goes to the user's legacy path`() {
        configure(iamEnabled = false)

        awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }

        assertThat(backend.requests.single().path).isEqualTo("/v1/subscribers/${encoded(currentAppUserID)}")
        assertNoIAMTraffic()
    }

    private fun assertNoIAMTraffic() {
        assertThat(backend.requests.map { it.path }).noneMatch {
            it.startsWith("/auth/") || it == "/v1/customer" || it.startsWith("/v1/customer/")
        }
        assertThat(backend.requests.map { it.authorization }).allMatch { it == "Bearer $API_KEY" }
    }

    private fun encoded(appUserID: String) = android.net.Uri.encode(appUserID)

    companion object {
        private const val PURCHASE_TIME = 1_792_000_000_000L
    }
}
