@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.iam

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.logInWith
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Scenario 4: an explicit login with an identity, from a bootstrapped anonymous user. */
internal class IAMLoginEndToEndTest : IAMEndToEndTest() {

    private val oidc = Identity.oidc("oidc-id-token".toByteArray())

    @Test
    fun `logging in links the anonymous identity and switches to the identified user`() {
        configure()
        foregroundAndBootstrap()
        val anonymousIDToken = backend.issuedTokens.single().getString("id_token")

        val result = awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }

        val login = backend.requests("/auth/login").last()
        assertThat(login.body?.getString("method")).isEqualTo("oidc")
        assertThat(login.body?.getString("id_token")).isEqualTo("oidc-id-token")
        assertThat(login.body?.getString("link_to_id")).isEqualTo(anonymousIDToken)
        assertThat(login.authorization).isEqualTo("Bearer $API_KEY")
        assertThat(currentAppUserID).isEqualTo(IDENTIFIED)
        assertThat(result.getOrThrow().originalAppUserId).isEqualTo(IDENTIFIED)
        assertThat(lastCallbackThread).isEqualTo(mainThread)
        assertThat(purchases.isAnonymous).isFalse()
    }

    @Test
    fun `after the login, requests use the identified user's token and listeners hear about them`() {
        configure()
        foregroundAndBootstrap()
        val before = backend.requests.size

        awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }
        awaitQuiet()

        val afterLogin = backend.requests.drop(before).dropWhile { it.path != "/auth/login" }.drop(1)
        assertThat(afterLogin).isNotEmpty
        assertThat(afterLogin.map { it.user() }).containsOnly(IDENTIFIED)
        assertThat(customerInfoUpdates.last().originalAppUserId).isEqualTo(IDENTIFIED)
    }

    @Test
    fun `the delegate hears the identified user's token exactly once`() {
        configure()
        foregroundAndBootstrap()

        awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }
        awaitQuiet()

        assertThat(delegate.tokens.map { backend.ownerOf(it) }).containsExactly(anonymousUser(), IDENTIFIED)
        assertThat(delegate.errors).isEmpty()
    }

    @Test
    fun `the delegate still hears the new token when the CustomerInfo fetch after the switch fails`() {
        configure()
        foregroundAndBootstrap()
        backend.respondOnce("/v1/customer") { MockResponse().setResponseCode(SERVER_ERROR) }

        val result = awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }
        awaitQuiet()

        assertThat(result.isFailure).isTrue()
        assertThat(currentAppUserID).isEqualTo(IDENTIFIED)
        assertThat(delegate.tokens.map { backend.ownerOf(it) }.last()).isEqualTo(IDENTIFIED)
        assertThat(delegate.tokens.count { backend.ownerOf(it) == IDENTIFIED }).isEqualTo(1)
    }

    @Test
    fun `unsynced attributes are synced as the anonymous user before the login`() {
        configure()
        foregroundAndBootstrap()
        val anonymous = currentAppUserID
        purchases.setAttributes(mapOf("favorite_cat" to "garfield"))

        val result = awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }

        assertThat(result.getOrThrow().originalAppUserId).isEqualTo(IDENTIFIED)
        assertThat(currentAppUserID).isEqualTo(IDENTIFIED)
        val paths = backend.requests.map { it.path }
        val attributes = backend.requests("/v1/customer/attributes").single()
        assertThat(attributes.user()).isEqualTo(anonymous)
        assertThat(paths.indexOf("/v1/customer/attributes")).isLessThan(paths.lastIndexOf("/auth/login"))
    }

    @Test
    fun `the old user's ETag is never sent for the new user`() {
        configure()
        foregroundAndBootstrap()
        val anonymous = currentAppUserID
        awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }.getOrThrow()

        awaitCustomerInfo { purchases.authentication.logIn(oidc, it) }.getOrThrow()

        // The anonymous re-fetch did send its ETag, so the check below isn't passing for lack of ETags.
        val anonymousFetches = backend.requests("/v1/customer").filter { it.user() == anonymous }
        assertThat(anonymousFetches.map { it.eTag }).contains("etag-$anonymous")
        val identifiedFetches = backend.requests("/v1/customer").filter { it.user() == IDENTIFIED }
        assertThat(identifiedFetches).isNotEmpty
        assertThat(identifiedFetches.map { it.eTag }).doesNotContain("etag-$anonymous")
    }

    @Test
    fun `a login by app user ID under IAM still goes to the legacy identify endpoint`() {
        // Pins today's behavior; Step B rejects this with UnsupportedError.
        configure()
        foregroundAndBootstrap()
        val anonymous = currentAppUserID
        var result: Result<Pair<String, Boolean>>? = null

        purchases.logInWith(
            "custom-id",
            onError = { result = Result.failure(PurchasesErrorException(it)) },
        ) { customerInfo, created -> result = Result.success(customerInfo.originalAppUserId to created) }
        awaitUntil("the logIn(String) result") { result != null }

        assertThat(result?.getOrThrow()).isEqualTo("custom-id" to true)
        assertThat(currentAppUserID).isEqualTo("custom-id")
        val identify = backend.requests("/v1/subscribers/identify").single()
        assertThat(identify.body?.getString("app_user_id")).isEqualTo(anonymous)
        assertThat(identify.body?.getString("new_app_user_id")).isEqualTo("custom-id")
    }

    private fun anonymousUser(): String = backend.ownerOf(backend.issuedTokens.first().getString("access_token"))!!

    private val mainThread: Thread
        get() = android.os.Looper.getMainLooper().thread

    companion object {
        private const val IDENTIFIED = "identified-user"
        private const val SERVER_ERROR = 500
    }
}
