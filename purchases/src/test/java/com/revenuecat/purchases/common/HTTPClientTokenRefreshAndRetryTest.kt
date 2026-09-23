package com.revenuecat.purchases.common

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.networking.TokenRefreshOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.mockwebserver.Dispatcher as MockWebServerDispatcher

/**
 * Tests for HTTPClient's 401-refresh-and-retry (IAM phase 5, step 22): the highest-risk piece of this
 * phase, since it's the one place HTTPClient recurses into itself based on a runtime response rather than
 * a caller-supplied flag. [TokenManager] already owns dedup/waiter-notification for concurrent refreshes
 * ([TokenManager.tokenRefreshRequest]/[TokenManager.handleTokenRefreshResponse]) -- these tests exercise
 * that real state machine, not a re-implementation of it.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class HTTPClientTokenRefreshAndRetryTest : BaseHTTPClientTest() {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val headers = mapOf("Authorization" to "Bearer api-key")

    @After
    fun cleanupObjectMocks() {
        unmockkObject(TokenRefreshOperation)
    }

    @Before
    fun setupSigningManager() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
    }

    private fun tokenManager(
        enabled: Boolean = true,
        appUserID: String? = "user",
        accessToken: String? = "old-access-token",
        refreshToken: String? = "old-refresh-token",
    ): TokenManager {
        val manager = TokenManager(context, "test_api_key", enabled = enabled, scope = testScope())
        if (appUserID != null && accessToken != null && refreshToken != null) {
            runBlocking {
                manager.saveTokens(appUserID, accessToken = accessToken, refreshToken = refreshToken, idToken = "old-id-token")
            }
        }
        return manager
    }

    private fun refreshResponseBody(
        accessToken: String = "new-access-token",
        refreshToken: String = "new-refresh-token",
        idToken: String = "new-id-token",
    ): String = JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken)
        .put("id_token", idToken)
        .toString()

    // region single refresh-and-retry

    @Test
    fun `a 401 triggers a refresh and retries the original request once`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        enqueue("/v1/customer", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))
        enqueue("/auth/token", HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, refreshResponseBody()))
        enqueue("/v1/customer", HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, "{}"))

        val result = client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = "user")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(server.requestCount).isEqualTo(3)
        server.takeRequest() // original, 401
        val refreshRequest = server.takeRequest()
        assertThat(refreshRequest.path).isEqualTo("/auth/token")
        val retriedRequest = server.takeRequest()
        assertThat(retriedRequest.getHeader("Authorization")).isEqualTo("Bearer new-access-token")
        assertThat(runBlocking { tokenManager.currentAccessToken("user") }).isEqualTo("new-access-token")
        assertThat(runBlocking { tokenManager.currentRefreshToken("user") }).isEqualTo("new-refresh-token")
    }

    // endregion

    // region concurrent 401s

    @Test
    fun `concurrent 401s for the same user trigger a single refresh, and both are retried`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        val customerInfoRequestCount = AtomicInteger(0)
        server.dispatcher = object : MockWebServerDispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path == "/v1/customer") {
                    val ordinal = customerInfoRequestCount.incrementAndGet()
                    if (ordinal <= 2) {
                        MockResponse().setResponseCode(RCHTTPStatusCodes.UNAUTHORIZED).setBody("{}")
                    } else {
                        MockResponse().setResponseCode(RCHTTPStatusCodes.SUCCESS).setBody("{}")
                    }
                } else {
                    MockResponse().setResponseCode(404)
                }
            }
        }
        // Concurrent, dynamically-numbered responses defeat enqueue()'s literal-argument ETagManager
        // stubbing (it can only match one fixed responseCode/payload per urlString), so this test stubs a
        // generic passthrough instead, built from whatever responseCode/payload the real request actually
        // got back from the dispatcher above.
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val responseCode = invocation.args[0] as Int
            val payload = invocation.args[1] as String
            HTTPResult.createResult(responseCode = responseCode, payload = payload)
        }
        mockkObject(TokenRefreshOperation)
        every {
            TokenRefreshOperation.refresh(any(), any(), any(), any(), any())
        } answers {
            Thread.sleep(200)
            TokenManager.TokenSet("new-access-token", "new-refresh-token", "new-id-token")
        }
        val executor = Executors.newFixedThreadPool(2)

        val futures = (1..2).map {
            executor.submit(
                Callable {
                    client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = "user")
                },
            )
        }
        val results = futures.map { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()

        assertThat(results).allMatch { it.responseCode == RCHTTPStatusCodes.SUCCESS }
        assertThat(customerInfoRequestCount.get()).isEqualTo(4) // 2 originals (401) + 2 retries (200)
        verify(exactly = 1) { TokenRefreshOperation.refresh(any(), any(), any(), any(), any()) }
        assertThat(runBlocking { tokenManager.currentAccessToken("user") }).isEqualTo("new-access-token")
    }

    // endregion

    // region refresh failure

    @Test
    fun `a failed refresh returns the original 401, without retrying or looping`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        enqueue("/v1/customer", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))
        enqueue(
            "/auth/token",
            HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, """{"code": 7224, "message": "invalid refresh token"}"""),
        )

        val result = client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = "user")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(runBlocking { tokenManager.currentAccessToken("user") }).isEqualTo("old-access-token")
    }

    // endregion

    // region already retried / auth endpoints / disabled -- no refresh attempted

    @Test
    fun `an already-retried request never triggers a second refresh`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        enqueue("/v1/customer", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))

        val result = client.performRequest(
            baseURL,
            Endpoint.GetCustomerInfo("user"),
            null,
            null,
            headers,
            appUserID = "user",
            retriedAfterTokenRefresh = true,
        )

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 from an auth endpoint never triggers a refresh`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        enqueue(Endpoint.TokenLogin.getPath(), HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))

        val result = client.performRequest(baseURL, Endpoint.TokenLogin, null, null, headers, appUserID = "user")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 with no tokenManager injected never triggers a refresh`() {
        client = createClient(tokenManager = null)
        enqueue("/v1/subscribers/user", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))

        val result = client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = "user")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 with a disabled tokenManager never triggers a refresh`() {
        val tokenManager = tokenManager(enabled = false)
        client = createClient(tokenManager = tokenManager)
        enqueue("/v1/subscribers/user", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))

        val result = client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = "user")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 with a null appUserID never triggers a refresh`() {
        val tokenManager = tokenManager()
        client = createClient(tokenManager = tokenManager)
        enqueue("/v1/customer", HTTPResult.createResult(RCHTTPStatusCodes.UNAUTHORIZED, "{}"))

        val result = client.performRequest(baseURL, Endpoint.GetCustomerInfo("user"), null, null, headers, appUserID = null)

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.UNAUTHORIZED)
        assertThat(server.requestCount).isEqualTo(1)
    }

    // endregion

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
}
