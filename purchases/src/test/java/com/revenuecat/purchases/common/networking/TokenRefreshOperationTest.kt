@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.createResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.URL

/** Tests for [TokenRefreshOperation]: the `/auth/token` networking primitive, called directly (no `Dispatcher`). */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenRefreshOperationTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val baseURL = URL("http://mock-api-test.revenuecat.com/")
    private val authHeaders = mapOf("Authorization" to "Bearer test_api_key")
    private var mockClient: HTTPClient = mockk()
    private val request = TokenManager.TokenRefreshRequest(appUserID = "user", refreshToken = "old-refresh-token")

    // region request shape

    @Test
    fun `refresh sends the right grant_type and refresh_token`() {
        mockResponse(200, validRefreshResponseBody())

        TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        verify(exactly = 1) {
            mockClient.performRequest(
                baseURL,
                Endpoint.TokenRefresh,
                mapOf("grant_type" to "refresh_token", "refresh_token" to "old-refresh-token"),
                null,
                authHeaders,
                any(),
                emptyList(),
                0,
            )
        }
    }

    // endregion

    // region success

    @Test
    fun `a successful response returns the refreshed token set`() {
        mockResponse(
            200,
            JSONObject()
                .put("access_token", "new-access")
                .put("refresh_token", "new-refresh")
                .put("id_token", "new-id")
                .toString(),
        )

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isEqualTo(TokenManager.TokenSet("new-access", "new-refresh", "new-id"))
    }

    @Test
    fun `a successful refresh's result correctly feeds TokenManager handleTokenRefreshResponse`() = runTest {
        mockResponse(200, validRefreshResponseBody())
        val tokenManager = TokenManager(context, "test_api_key", enabled = true)
        tokenManager.saveTokens(
            "user",
            accessToken = "old-access",
            refreshToken = "old-refresh-token",
            idToken = "old-id",
        )

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)
        tokenManager.handleTokenRefreshResponse("user", result)

        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("new-access")
        assertThat(tokenManager.currentRefreshToken("user")).isEqualTo("new-refresh")
        assertThat(tokenManager.currentIDToken("user")).isEqualTo("new-id")
    }

    // endregion

    // region failure

    @Test
    fun `an unsuccessful http response returns null, rather than throwing`() {
        mockResponse(401, """{"code": 7224, "message": "invalid refresh token"}""")

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isNull()
    }

    @Test
    fun `a response missing a token field returns null`() {
        mockResponse(200, JSONObject().put("access_token", "new-access").put("id_token", "new-id").toString())

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isNull()
    }

    @Test
    fun `a connection failure returns null, rather than propagating the exception`() {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } throws IOException("no connection")

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isNull()
    }

    @Test
    fun `a JSON encoding failure returns null, rather than propagating the exception`() {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } throws JSONException("bad json")

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isNull()
    }

    @Test
    fun `an unexpected exception type also returns null, rather than propagating`() {
        // An exception besides IOException/JSONException must also return null, not propagate.
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } throws SecurityException("INTERNET permission revoked")

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)

        assertThat(result).isNull()
    }

    @Test
    fun `a failed refresh does not update TokenManager when fed into handleTokenRefreshResponse`() = runTest {
        mockResponse(401, """{"code": 7224, "message": "invalid refresh token"}""")
        val tokenManager = TokenManager(context, "test_api_key", enabled = true)
        tokenManager.saveTokens(
            "user",
            accessToken = "old-access",
            refreshToken = "old-refresh-token",
            idToken = "old-id",
        )

        val result = TokenRefreshOperation.refresh(baseURL, mockClient, authHeaders, request)
        tokenManager.handleTokenRefreshResponse("user", result)

        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("old-access")
        assertThat(tokenManager.currentRefreshToken("user")).isEqualTo("old-refresh-token")
    }

    // endregion

    private fun mockResponse(responseCode: Int, body: String) {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(responseCode, body)
    }

    private fun validRefreshResponseBody(): String =
        JSONObject()
            .put("access_token", "new-access")
            .put("refresh_token", "new-refresh")
            .put("id_token", "new-id")
            .toString()
}
