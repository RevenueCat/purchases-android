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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenAuthenticatorTest {

    private val endpoint = Endpoint.GetCustomerInfo("user")
    private val unauthorized = RCHTTPStatusCodes.UNAUTHORIZED
    private val noRefresh: (Map<String, Any?>) -> HTTPResult = { throw AssertionError("Unexpected refresh") }

    @Test
    fun `a successful refresh saves the new tokens and says to retry`() = runTest {
        val tokenManager = tokenManager()
        var postedBody: Map<String, Any?>? = null

        val retry = authenticator(tokenManager).refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
            postedBody = it
            tokenResponse("new-access")
        }

        assertThat(retry).isTrue()
        assertThat(postedBody).isEqualTo(TokenRefreshOperation.body("refresh-token"))
        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("new-access")
    }

    @Test
    fun `a failed refresh says not to retry and keeps the tokens`() = runTest {
        val tokenManager = tokenManager()

        val retry = authenticator(tokenManager).refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
            HTTPResult.createResult(responseCode = RCHTTPStatusCodes.UNAUTHORIZED)
        }

        assertThat(retry).isFalse()
        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("access")
    }

    @Test
    fun `non-401s, auth endpoints, retried requests and users without tokens never refresh`() = runTest {
        val authenticator = authenticator(tokenManager())
        val sent = bearer("access")

        assertThat(authenticator.refreshTokensIfNeeded(endpoint, RCHTTPStatusCodes.ERROR, false, sent, noRefresh))
            .isFalse()
        assertThat(authenticator.refreshTokensIfNeeded(Endpoint.TokenLogin, unauthorized, false, sent, noRefresh))
            .isFalse()
        assertThat(authenticator.refreshTokensIfNeeded(endpoint, unauthorized, true, sent, noRefresh)).isFalse()
        assertThat(
            TokenAuthenticator(tokenManager()) { "other-user" }
                .refreshTokensIfNeeded(endpoint, unauthorized, false, emptyMap(), noRefresh),
        ).isFalse()
    }

    @Test
    fun `a token refreshed by another request is reused without refreshing again`() = runTest {
        val tokenManager = tokenManager()
        tokenManager.saveTokens("user", "newer-access", "newer-refresh", "id")

        val retry = authenticator(tokenManager)
            .refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access"), noRefresh)

        assertThat(retry).isTrue()
    }

    @Test
    fun `a request sent with the API key retries once a token is available`() = runTest {
        val retry = authenticator(tokenManager())
            .refreshTokensIfNeeded(endpoint, unauthorized, false, emptyMap(), noRefresh)

        assertThat(retry).isTrue()
    }

    @Test
    fun `concurrent 401s for the same token cause one refresh, and both retry`() = runTest {
        val authenticator = authenticator(tokenManager())
        val refreshes = AtomicInteger()
        val refreshStarted = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        val firstRetried = AtomicBoolean()
        val secondRetried = AtomicBoolean()

        val first = thread {
            firstRetried.set(
                authenticator.refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
                    refreshes.incrementAndGet()
                    refreshStarted.countDown()
                    releaseRefresh.await(5, TimeUnit.SECONDS)
                    tokenResponse("new-access")
                },
            )
        }
        assertThat(refreshStarted.await(5, TimeUnit.SECONDS)).isTrue()
        val second = thread {
            secondRetried.set(
                authenticator.refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
                    refreshes.incrementAndGet()
                    tokenResponse("second-access")
                },
            )
        }
        waitUntilBlocked(second)
        releaseRefresh.countDown()
        first.join(5_000)
        second.join(5_000)

        assertThat(refreshes.get()).isEqualTo(1)
        assertThat(firstRetried.get()).isTrue()
        assertThat(secondRetried.get()).isTrue()
    }

    @Test
    fun `an exception while refreshing releases the lock`() = runTest {
        val authenticator = authenticator(tokenManager())

        assertThatThrownBy {
            authenticator.refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
                throw IllegalStateException("boom")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        val retry = authenticator.refreshTokensIfNeeded(endpoint, unauthorized, false, bearer("access")) {
            tokenResponse("new-access")
        }
        assertThat(retry).isTrue()
    }

    private fun authenticator(tokenManager: TokenManager) = TokenAuthenticator(tokenManager) { "user" }

    private fun bearer(token: String) = mapOf("Authorization" to "Bearer $token")

    private fun tokenResponse(accessToken: String) = HTTPResult.createResult(
        payload = JSONObject().put("access_token", accessToken).put("refresh_token", "refresh-$accessToken").toString(),
    )

    // Blocked on refreshLock's monitor, i.e. waiting on the other thread's refresh.
    private fun waitUntilBlocked(thread: Thread) {
        val deadline = System.currentTimeMillis() + 5_000
        while (thread.state != Thread.State.BLOCKED) {
            check(System.currentTimeMillis() < deadline) { "Thread never blocked on the refresh lock" }
            Thread.yield()
        }
    }

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
