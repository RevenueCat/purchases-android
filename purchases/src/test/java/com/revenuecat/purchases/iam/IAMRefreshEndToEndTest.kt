package com.revenuecat.purchases.iam

import com.revenuecat.purchases.CacheFetchPolicy
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

/** Scenario 5: a 401 refreshes the access token and retries; failures reach the delegate. */
internal class IAMRefreshEndToEndTest : IAMEndToEndTest() {

    @Test
    fun `a 401 refreshes the token, retries once and publishes the new token`() {
        configure()
        foregroundAndBootstrap()
        val user = currentAppUserID
        val oldToken = delegate.tokens.single()!!
        backend.expire(oldToken)

        val result = awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }
        awaitQuiet()

        assertThat(result.isSuccess).isTrue()
        val refresh = backend.requests("/auth/token").single()
        assertThat(refresh.authorization).isEqualTo("Bearer $API_KEY")
        assertThat(refresh.body?.getString("refresh_token")).isEqualTo(issuedRefreshToken(0))
        val fetches = backend.requests("/v1/customer").takeLast(2)
        assertThat(fetches.map { it.bearerToken }).containsExactly(oldToken, delegate.tokens.last())
        assertThat(delegate.tokens.map { backend.ownerOf(it) }).containsExactly(user, user)
        assertThat(purchases.authentication.currentAccessToken).isNotEqualTo(oldToken)
    }

    @Test
    fun `several requests failing with the same expired token refresh once`() {
        configure()
        foregroundAndBootstrap()
        backend.expire(delegate.tokens.single()!!)

        purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, noopCallback())
        purchases.getOfferings(noopOfferingsCallback())
        purchases.syncAttributesAndOfferingsIfNeeded(noopSyncCallback())
        awaitQuiet()

        assertThat(backend.requests("/auth/token")).hasSize(1)
    }

    @Test
    fun `a failed refresh reaches the delegate and fails the request`() {
        configure()
        foregroundAndBootstrap()
        backend.expire(delegate.tokens.single()!!)
        backend.respondOnce("/auth/token") { MockResponse().setResponseCode(SERVER_ERROR) }

        val result = awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }
        awaitQuiet()

        assertThat(result.isFailure).isTrue()
        assertThat(delegate.errors).hasSize(1)
    }

    @Test
    fun `a refresh response without a refresh token leaves the next 401 unrecoverable`() {
        // Pins today's behavior (Open question 1): the missing refresh token clears the stored one.
        configure()
        foregroundAndBootstrap()
        val user = currentAppUserID
        backend.expire(delegate.tokens.single()!!)
        backend.respondOnce("/auth/token") {
            val issued = backend.tokensFor(user, listOf("anonymous"))
            MockResponse().setResponseCode(OK)
                .setBody(JSONObject().put("access_token", issued.getString("access_token")).toString())
        }
        awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }
        awaitQuiet()

        backend.expire(purchases.authentication.currentAccessToken!!)
        val second = awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }
        awaitQuiet()

        assertThat(second.isFailure).isTrue()
        assertThat(backend.requests("/auth/token")).hasSize(1)
    }

    @Test
    fun `a 401 without a body isn't refreshed`() {
        // Pins a gap: HTTPClient fails a bodyless response as a network error before the refresh check.
        configure()
        foregroundAndBootstrap()
        backend.respondOnce("/v1/customer") { MockResponse().setResponseCode(UNAUTHORIZED) }

        val result = awaitCustomerInfo { purchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, it) }
        awaitQuiet()

        assertThat(result.isFailure).isTrue()
        assertThat(backend.requests("/auth/token")).isEmpty()
    }

    private fun issuedRefreshToken(index: Int): String = backend.issuedTokens[index].getString("refresh_token")

    companion object {
        private const val OK = 200
        private const val UNAUTHORIZED = 401
        private const val SERVER_ERROR = 500
    }
}
