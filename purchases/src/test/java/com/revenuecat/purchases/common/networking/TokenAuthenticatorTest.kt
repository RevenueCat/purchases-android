@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.networking

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.createResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenAuthenticatorTest {

    private val endpoint = Endpoint.GetCustomerInfo("user")

    @Test
    fun `a successful refresh says to retry`() = runTest {
        val authenticator = authenticator(tokenManager())
        var postedBody: Map<String, Any?>? = null

        val retry = authenticator.refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.UNAUTHORIZED, false) {
            postedBody = it
            HTTPResult.createResult(payload = JSONObject().put("access_token", "new-access").toString())
        }

        assertThat(retry).isTrue()
        assertThat(postedBody).isEqualTo(TokenRefreshOperation.body("refresh-token"))
    }

    @Test
    fun `a failed refresh says not to retry`() = runTest {
        val authenticator = authenticator(tokenManager())

        val retry = authenticator.refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.UNAUTHORIZED, false) {
            HTTPResult.createResult(responseCode = RCHTTPStatusCodes.UNAUTHORIZED)
        }

        assertThat(retry).isFalse()
    }

    @Test
    fun `non-401 responses, auth endpoints and a user without tokens never refresh`() = runTest {
        val tokenManager = tokenManager()
        val post: (Map<String, Any?>) -> HTTPResult = { throw AssertionError("Unexpected refresh") }

        assertThat(authenticator(tokenManager).refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.ERROR, false, post))
            .isFalse()
        assertThat(
            authenticator(tokenManager)
                .refreshTokensIfNeeded(Endpoint.TokenLogin, RCHTTPStatusCodes.UNAUTHORIZED, false, post),
        ).isFalse()
        assertThat(
            TokenAuthenticator(tokenManager) { "other-user" }
                .refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.UNAUTHORIZED, false, post),
        ).isFalse()
    }

    @Test
    fun `an unexpected exception while refreshing still resolves the refresh`() = runTest {
        val tokenManager = tokenManager()
        val authenticator = authenticator(tokenManager)

        assertThatThrownBy {
            authenticator.refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.UNAUTHORIZED, false) {
                throw IllegalStateException("boom")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        // Not left waiting on the refresh that threw.
        val next = tokenManager.tokenRefreshRequest("user", RCHTTPStatusCodes.UNAUTHORIZED, false) {}
        assertThat(next).isInstanceOf(TokenManager.TokenRefreshAction.Refresh::class.java)
    }

    private fun authenticator(tokenManager: TokenManager) = TokenAuthenticator(tokenManager) { "user" }

    private fun TestScope.tokenManager(): TokenManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TokenManager(
            ApplicationProvider.getApplicationContext(),
            "test_api_key",
            enabled = true,
            computationDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        ).apply {
            advanceUntilIdle()
            saveTokens("user", "access", "refresh-token", "id")
        }
    }
}
