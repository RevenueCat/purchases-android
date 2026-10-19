@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.identity.Identity
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Scenario 6: logging out revokes the refresh token and logs in anonymously. */
internal class IAMLogoutEndToEndTest : IAMEndToEndTest() {

    @Test
    fun `logging out revokes the identified user's tokens and becomes a new anonymous user`() {
        logInIdentified()
        val identifiedRefreshToken = backend.issuedTokens.last().getString("refresh_token")

        val result = awaitCustomerInfo { purchases.authentication.logOut(it) }
        awaitQuiet()

        val revoke = backend.requests("/auth/revoke").single()
        assertThat(revoke.authorization).isEqualTo("Bearer $API_KEY")
        assertThat(revoke.body?.getString("token")).isEqualTo(identifiedRefreshToken)
        val paths = backend.requests.map { it.path }
        assertThat(paths.lastIndexOf("/auth/login")).isGreaterThan(paths.indexOf("/auth/revoke"))
        // Today the anonymous login starts from the identified user, so nothing is migrated (Step C changes this).
        assertThat(backend.requests("/auth/login").last().body?.has("app_user_id")).isFalse()
        assertThat(currentAppUserID).startsWith("server-anonymous-")
        assertThat(purchases.isAnonymous).isTrue()
        assertThat(result.getOrThrow().originalAppUserId).isEqualTo(currentAppUserID)
        assertThat(lastCallbackThread).isEqualTo(android.os.Looper.getMainLooper().thread)
        assertThat(backend.ownerOf(delegate.tokens.last())).isEqualTo(currentAppUserID)
    }

    @Test
    fun `a failed revoke changes nothing`() {
        logInIdentified()
        val token = purchases.authentication.currentAccessToken
        backend.respondOnce("/auth/revoke") { MockResponse().setResponseCode(SERVER_ERROR).setBody("{}") }

        val result = awaitCustomerInfo { purchases.authentication.logOut(it) }
        awaitQuiet()

        assertThat(result.isFailure).isTrue()
        assertThat(lastCallbackThread).isEqualTo(android.os.Looper.getMainLooper().thread)
        assertThat(currentAppUserID).isEqualTo(IDENTIFIED)
        assertThat(purchases.authentication.currentAccessToken).isEqualTo(token)
        assertThat(backend.requests("/auth/login")).hasSize(2)
    }

    @Test
    fun `a failed anonymous login after the revoke falls back to a local anonymous ID`() {
        logInIdentified()
        backend.respondOnce("/auth/login") { MockResponse().setResponseCode(SERVER_ERROR).setBody("{}") }

        val result = awaitCustomerInfo { purchases.authentication.logOut(it) }
        awaitQuiet()

        assertThat(currentAppUserID).startsWith(LOCAL_ANONYMOUS_PREFIX)
        assertThat(purchases.authentication.currentAccessToken).isNull()
        // Pins Open question 8: the token-less user's CustomerInfo goes to the user-less IAM path with only the
        // API key, so the logout's result is whatever the server makes of that (an error here).
        val fetch = backend.requests("/v1/customer").last()
        assertThat(fetch.authorization).isEqualTo("Bearer $API_KEY")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `after the fallback, the next foreground bootstraps the local anonymous ID`() {
        logInIdentified()
        backend.respondOnce("/auth/login") { MockResponse().setResponseCode(SERVER_ERROR).setBody("{}") }
        awaitCustomerInfo { purchases.authentication.logOut(it) }
        awaitQuiet()
        val localID = currentAppUserID

        background()
        foregroundAndBootstrap()

        assertThat(backend.requests("/auth/login").last().body?.getString("app_user_id")).isEqualTo(localID)
        assertThat(currentAppUserID).isEqualTo(localID)
    }

    @Test
    fun `logging out an anonymous user fails without revoking`() {
        configure()
        foregroundAndBootstrap()

        val result = awaitCustomerInfo { purchases.authentication.logOut(it) }

        assertThat(result.purchasesError?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
        assertThat(backend.requests("/auth/revoke")).isEmpty()
    }

    @Test
    fun `logging out a server-assigned anonymous user fails too`() {
        backend.adoptsAnonymousAppUserID = false
        configure()
        foregroundAndBootstrap()

        val result = awaitCustomerInfo { purchases.authentication.logOut(it) }

        assertThat(result.purchasesError?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
        assertThat(backend.requests("/auth/revoke")).isEmpty()
    }

    private fun logInIdentified() {
        configure()
        foregroundAndBootstrap()
        awaitCustomerInfo { purchases.authentication.logIn(Identity.oidc("oidc-id-token".toByteArray()), it) }
        awaitQuiet()
        check(currentAppUserID == IDENTIFIED)
    }

    companion object {
        private const val IDENTIFIED = "identified-user"
        private const val SERVER_ERROR = 500
        private const val LOCAL_ANONYMOUS_PREFIX = "\$RCAnonymousID:"
    }
}
