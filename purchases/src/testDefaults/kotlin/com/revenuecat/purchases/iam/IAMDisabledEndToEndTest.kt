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
    fun `login, purchase, restore and logout succeed on the legacy endpoints`() {
        configure(iamEnabled = false)
        foreground()
        awaitQuiet()
        val anonymousID = currentAppUserID

        var loginResult: Result<Pair<String, Boolean>>? = null
        purchases.logInWith(
            "custom-id",
            onError = { loginResult = Result.failure(PurchasesErrorException(it)) },
        ) { customerInfo, created -> loginResult = Result.success(customerInfo.originalAppUserId to created) }
        awaitUntil("the login") { loginResult != null }
        assertThat(loginResult?.getOrThrow()).isEqualTo("custom-id" to true)
        assertThat(currentAppUserID).isEqualTo("custom-id")
        val identify = backend.requests("/v1/subscribers/identify").single()
        assertThat(identify.body?.getString("app_user_id")).isEqualTo(anonymousID)
        assertThat(identify.body?.getString("new_app_user_id")).isEqualTo("custom-id")

        billing.deliverPurchase(stubStoreTransactionFromGooglePurchase(listOf("monthly"), PURCHASE_TIME))
        awaitUntil("the purchase's receipt post") { backend.requests("/v1/receipts").isNotEmpty() }
        assertThat(backend.requests("/v1/receipts").single().body?.getString("app_user_id")).isEqualTo("custom-id")

        val restored = awaitCustomerInfo { purchases.restorePurchases(it) }
        assertThat(restored.getOrThrow().originalAppUserId).isEqualTo("custom-id")
        assertThat(backend.requests("/v1/receipts").last().body?.getBoolean("is_restore")).isTrue()

        val loggedOut = awaitCustomerInfo { purchases.logOut(it) }
        awaitQuiet()
        assertThat(currentAppUserID).startsWith("\$RCAnonymousID:").isNotEqualTo(anonymousID)
        assertThat(loggedOut.getOrThrow().originalAppUserId).isEqualTo(currentAppUserID)
        assertThat(purchases.isAnonymous).isTrue()

        assertNoIAMTraffic()
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

        val result = awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }

        assertThat(result.getOrThrow().originalAppUserId).isEqualTo(currentAppUserID)
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
