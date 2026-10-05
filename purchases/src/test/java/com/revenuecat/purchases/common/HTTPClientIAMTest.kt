@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.TokenAuthenticator
import com.revenuecat.purchases.common.networking.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as AnnotationConfig

/** IAM behavior of [HTTPClient]: access-token auth, IAM paths, and 401 refresh-and-retry. */
@RunWith(AndroidJUnit4::class)
@AnnotationConfig(manifest = AnnotationConfig.NONE)
internal class HTTPClientIAMTest : BaseHTTPClientTest() {

    private val appUserID = "user"
    private val apiKeyHeaders = mapOf("Authorization" to "Bearer api_key")
    private val customerInfo = Endpoint.GetCustomerInfo(appUserID)
    private val unauthorized = HTTPResult.createResult(
        responseCode = RCHTTPStatusCodes.UNAUTHORIZED,
        payload = """{"code":7224,"message":"Access token expired"}""",
    )

    private lateinit var tokenManager: TokenManager

    @Before
    fun setupSigning() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
    }

    // region auth header and path

    @Test
    fun `without a TokenManager requests are unchanged`() {
        client = createClient()
        enqueue(customerInfo.getPath(), HTTPResult.createResult())

        performRequest(customerInfo)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/subscribers/user")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer api_key")
    }

    @Test
    fun `a disabled TokenManager leaves requests unchanged`() = runTest {
        client = iamClient(enabled = false)
        enqueue(customerInfo.getPath(), HTTPResult.createResult())

        performRequest(customerInfo)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/subscribers/user")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer api_key")
    }

    @Test
    fun `with no stored token the API key is used, on the IAM path`() = runTest {
        client = iamClient()
        enqueue("/v1/customer", HTTPResult.createResult())

        performRequest(customerInfo)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/customer")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer api_key")
    }

    @Test
    fun `a stored access token replaces the API key`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "access-token", "refresh-token", "id-token")
        enqueue("/v1/customer", HTTPResult.createResult())

        performRequest(customerInfo)

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer access-token")
    }

    @Test
    fun `another user's stored token is never sent`() = runTest {
        client = iamClient(currentUser = "other-user")
        tokenManager.saveTokens(appUserID, "access-token", "refresh-token", "id-token")
        enqueue("/v1/customer", HTTPResult.createResult())

        performRequest(customerInfo)

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer api_key")
    }

    @Test
    fun `auth endpoints always use the API key`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "access-token", "refresh-token", "id-token")
        enqueue("/auth/login", HTTPResult.createResult())

        performRequest(Endpoint.TokenLogin)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/auth/login")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer api_key")
    }

    @Test
    fun `migrated endpoints use their IAM paths`() = runTest {
        client = iamClient()
        enqueue("/v1/customer/offerings", HTTPResult.createResult())

        performRequest(Endpoint.GetOfferings(appUserID))

        assertThat(server.takeRequest().path).isEqualTo("/v1/customer/offerings")
    }

    // endregion

    // region refresh and retry

    @Test
    fun `a 401 refreshes the tokens and retries once with the new access token`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/v1/customer", unauthorized)
        enqueue("/auth/token", tokenResponse("new-access", "new-refresh", "new-id"))
        enqueue("/v1/customer", HTTPResult.createResult())

        val result = performRequest(customerInfo)

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer old-access")
        val refresh = server.takeRequest()
        assertThat(refresh.path).isEqualTo("/auth/token")
        assertThat(refresh.getHeader("Authorization")).isEqualTo("Bearer api_key")
        assertThat(JSONObject(refresh.body.readUtf8()).getString("refresh_token")).isEqualTo("refresh-token")
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer new-access")
        assertThat(tokenManager.currentRefreshToken(appUserID)).isEqualTo("new-refresh")
    }

    @Test
    fun `a failed refresh returns the original 401 without retrying`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/v1/customer", unauthorized)
        enqueue("/auth/token", HTTPResult.createResult(responseCode = RCHTTPStatusCodes.UNAUTHORIZED))

        val result = performRequest(customerInfo)

        assertThat(result).isEqualTo(unauthorized)
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(tokenManager.currentAccessToken(appUserID)).isEqualTo("old-access")
    }

    @Test
    fun `a refresh that can't reach the server returns the original 401`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/v1/customer", unauthorized)
        // HttpURLConnection retries a dropped connection once.
        repeat(2) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)) }

        val result = performRequest(customerInfo)

        assertThat(result).isEqualTo(unauthorized)
        assertThat(tokenManager.currentAccessToken(appUserID)).isEqualTo("old-access")
    }

    @Test
    fun `a 401 on the retried request does not refresh again`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/v1/customer", unauthorized)
        enqueue("/auth/token", tokenResponse("new-access", "new-refresh", "new-id"))
        enqueue("/v1/customer", unauthorized)

        val result = performRequest(customerInfo)

        assertThat(result).isEqualTo(unauthorized)
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `a 401 without a refresh token is returned as is`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", refreshToken = null, idToken = null)
        enqueue("/v1/customer", unauthorized)

        assertThat(performRequest(customerInfo)).isEqualTo(unauthorized)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 from an auth endpoint never triggers a refresh`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/auth/login", unauthorized)

        assertThat(performRequest(Endpoint.TokenLogin)).isEqualTo(unauthorized)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 with IAM disabled is returned as is`() = runTest {
        client = iamClient(enabled = false)
        enqueue(customerInfo.getPath(), unauthorized)

        assertThat(performRequest(customerInfo)).isEqualTo(unauthorized)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `a 401 after another request already refreshed retries with the new token, without refreshing`() = runTest {
        client = iamClient()
        tokenManager.saveTokens(appUserID, "old-access", "refresh-token", "old-id")
        enqueue("/v1/customer", unauthorized)
        enqueue("/v1/customer", HTTPResult.createResult())
        // Another request's refresh lands while this one is in flight.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.getHeader("Authorization") == "Bearer old-access") {
                    tokenManager.saveTokens(appUserID, "new-access", "new-refresh", "new-id")
                    MockResponse().setResponseCode(RCHTTPStatusCodes.UNAUTHORIZED).setBody(unauthorized.payloadText)
                } else {
                    MockResponse().setBody("{}")
                }
        }

        val result = performRequest(customerInfo)

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer old-access")
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer new-access")
    }

    // endregion

    private fun performRequest(endpoint: Endpoint): HTTPResult =
        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, apiKeyHeaders)

    private fun TestScope.iamClient(enabled: Boolean = true, currentUser: String = appUserID): HTTPClient {
        val dispatcher = StandardTestDispatcher(testScheduler)
        tokenManager = TokenManager(
            ApplicationProvider.getApplicationContext(),
            "test_api_key",
            enabled,
            computationDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()
        return createClient(tokenAuthenticator = TokenAuthenticator(tokenManager) { currentUser })
    }

    private fun tokenResponse(accessToken: String, refreshToken: String, idToken: String) =
        HTTPResult.createResult(
            payload = JSONObject()
                .put("access_token", accessToken)
                .put("refresh_token", refreshToken)
                .put("id_token", idToken)
                .toString(),
        )
}
