package com.revenuecat.purchases.common

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for HTTPClient's IAM auth-header override (IAM phase 5, step 20): whether a request's
 * `Authorization` header comes from the injected [TokenManager] (a stored access token) or falls back to
 * `requestHeaders`' own API-key header, and that auth endpoints (`isIAMEndpoint`) are never overridden.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class HTTPClientIAMAuthHeaderTest : BaseHTTPClientTest() {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val apiKeyHeaders = mapOf("Authorization" to "Bearer api-key")

    @Before
    fun setupSigningManager() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
    }

    // region disabled / not wired up

    @Test
    fun `no tokenManager injected leaves the Authorization header unchanged`() {
        client = createClient(tokenManager = null)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = "user")

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api-key")
    }

    @Test
    fun `a disabled tokenManager leaves the Authorization header unchanged, even with a token stored`() {
        val tokenManager = tokenManager(enabled = false)
        client = createClient(tokenManager = tokenManager)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = "user")

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api-key")
    }

    @Test
    fun `a null appUserID leaves the Authorization header unchanged, even when enabled with a token stored`() {
        val tokenManager = tokenManager(enabled = true, appUserID = "user", accessToken = "stored-access-token")
        client = createClient(tokenManager = tokenManager)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = null)

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api-key")
    }

    // endregion

    // region enabled

    @Test
    fun `enabled with no stored token falls back to the API key`() {
        val tokenManager = tokenManager(enabled = true)
        client = createClient(tokenManager = tokenManager)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = "user")

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api-key")
    }

    @Test
    fun `enabled with a stored token overrides the API key with a Bearer token`() {
        val tokenManager = tokenManager(enabled = true, appUserID = "user", accessToken = "stored-access-token")
        client = createClient(tokenManager = tokenManager)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = "user")

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer stored-access-token")
    }

    @Test
    fun `a stored token for a different appUserID does not override this request's header`() {
        val tokenManager = tokenManager(enabled = true, appUserID = "someone-else", accessToken = "someone-elses-token")
        client = createClient(tokenManager = tokenManager)
        val endpoint = Endpoint.LogIn
        enqueue(endpoint.getPath(), HTTPResult.createResult())

        client.performRequest(baseURL, endpoint, null, null, apiKeyHeaders, appUserID = "user")

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api-key")
    }

    // endregion

    // region auth endpoints always use the API key

    @Test
    fun `auth endpoints always use the API key, even enabled with a token stored`() {
        val tokenManager = tokenManager(enabled = true, appUserID = "user", accessToken = "stored-access-token")
        client = createClient(tokenManager = tokenManager)

        for (endpoint in listOf(Endpoint.TokenLogin, Endpoint.TokenRefresh, Endpoint.TokenLogout)) {
            enqueue(endpoint.getPath(), HTTPResult.createResult())

            client.performRequest(
                baseURL,
                endpoint,
                null,
                null,
                apiKeyHeaders,
                appUserID = "user",
            )

            assertThat(server.takeRequest().getHeader("Authorization"))
                .withFailMessage { "Expected the API key for $endpoint, not a bearer token" }
                .isEqualTo("Bearer api-key")
        }
    }

    // endregion

    private fun tokenManager(
        enabled: Boolean,
        appUserID: String? = null,
        accessToken: String? = null,
    ): TokenManager {
        val manager = TokenManager(context, "test_api_key", enabled = enabled, scope = testScope())
        if (appUserID != null && accessToken != null) {
            runBlocking {
                manager.saveTokens(appUserID, accessToken = accessToken, refreshToken = "refresh", idToken = "id")
            }
        }
        return manager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
}
