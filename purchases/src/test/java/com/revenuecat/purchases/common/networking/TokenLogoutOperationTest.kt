@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.createResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL

/**
 * Tests for [TokenLogoutOperation] (IAM phase 5, step 18): the `/auth/revoke` networking primitive.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenLogoutOperationTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val baseURL = URL("http://mock-api-test.revenuecat.com/")
    private val authHeaders = mapOf("Authorization" to "Bearer test_api_key")
    private val dispatcher = SyncDispatcher()
    private var mockClient: HTTPClient = mockk()

    private var succeeded = false
    private var receivedError: PurchasesError? = null
    private val onSuccessHandler: () -> Unit = { succeeded = true }
    private val onErrorHandler: (PurchasesError) -> Unit = { receivedError = it }

    // region refresh token present

    @Test
    fun `a stored refresh token sends the right request`() = runTest {
        val tokenManager = tokenManagerWithTokens(refreshToken = "a-refresh-token")
        mockResponse(200, "{}")

        logout(tokenManager)

        verify(exactly = 1) {
            mockClient.performRequest(
                baseURL,
                Endpoint.TokenLogout,
                mapOf("token" to "a-refresh-token", "token_type_hint" to "refresh_token"),
                null,
                authHeaders,
                any(),
                emptyList(),
                0,
            )
        }
    }

    @Test
    fun `success clears all local tokens`() = runTest {
        val tokenManager = tokenManagerWithTokens(refreshToken = "a-refresh-token")
        mockResponse(200, "{}")

        logout(tokenManager)

        assertThat(succeeded).isTrue()
        assertThat(receivedError).isNull()
        assertThat(tokenManager.currentAccessToken("user")).isNull()
        assertThat(tokenManager.currentRefreshToken("user")).isNull()
        assertThat(tokenManager.currentIDToken("user")).isNull()
    }

    @Test
    fun `failure leaves tokens intact and propagates the error`() = runTest {
        val tokenManager = tokenManagerWithTokens(refreshToken = "a-refresh-token")
        mockResponse(400, """{"code": 7226, "message": "bad request"}""")

        logout(tokenManager)

        assertThat(succeeded).isFalse()
        assertThat(receivedError).isNotNull()
        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("access")
        assertThat(tokenManager.currentRefreshToken("user")).isEqualTo("a-refresh-token")
        assertThat(tokenManager.currentIDToken("user")).isEqualTo("id")
    }

    @Test
    fun `success does not clear tokens a newer session already replaced while revoke was in flight`() = runTest {
        val tokenManager = tokenManagerWithTokens(refreshToken = "old-refresh-token")
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            // Simulate a login/refresh whose result lands while this revoke's network round trip is still
            // in flight -- the exact race that used to make a successful revoke of the OLD refresh token
            // wipe out the session a newer operation had already started.
            tokenManager.saveTokensSync(
                "user",
                accessToken = "new-access",
                refreshToken = "new-refresh-token",
                idToken = "new-id",
            )
            HTTPResult.createResult(200, "{}")
        }

        logout(tokenManager)

        assertThat(succeeded).isTrue()
        assertThat(receivedError).isNull()
        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("new-access")
        assertThat(tokenManager.currentRefreshToken("user")).isEqualTo("new-refresh-token")
        assertThat(tokenManager.currentIDToken("user")).isEqualTo("new-id")
    }

    // endregion

    // region no refresh token stored

    @Test
    fun `no stored refresh token skips the network call and clears local state immediately`() = runTest {
        val tokenManager = tokenManagerWithTokens(refreshToken = null)

        logout(tokenManager)

        assertThat(succeeded).isTrue()
        assertThat(receivedError).isNull()
        verify(exactly = 0) { mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // endregion

    private fun logout(tokenManager: TokenManager) {
        TokenLogoutOperation.logout(
            baseURL,
            mockClient,
            dispatcher,
            authHeaders,
            tokenManager,
            appUserID = "user",
            onSuccessHandler = onSuccessHandler,
            onErrorHandler = onErrorHandler,
        )
    }

    private fun mockResponse(responseCode: Int, body: String) {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(responseCode, body)
    }

    private suspend fun tokenManagerWithTokens(refreshToken: String?): TokenManager {
        val tokenManager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        if (refreshToken != null) {
            tokenManager.saveTokens("user", accessToken = "access", refreshToken = refreshToken, idToken = "id")
        }
        return tokenManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
}
