@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.identity.Identity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Tests for [TokenAPI] (IAM phase 5, step 19): dedup of concurrent identical calls, token persistence on
 * a successful login, and error passthrough.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenAPITest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")
    private var mockClient: HTTPClient = mockk()
    private val mockAppConfig: AppConfig = mockk<AppConfig>().apply {
        every { baseURL } returns mockBaseURL
        every { fallbackBaseURLs } returns emptyList()
    }

    // region dedup

    @Test
    fun `logIn dedupes concurrent identical logins into a single underlying call`() {
        val asyncDispatcher = Dispatcher(ThreadPoolExecutor(1, 2, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()))
        val tokenAPI = tokenAPI(dispatcher = asyncDispatcher)
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            Thread.sleep(200)
            HTTPResult.createResult(200, validLoginResponseBody("new-app-user-id"))
        }
        val identity = Identity.google("a-google-token".toByteArray())
        val latch = CountDownLatch(2)

        repeat(2) {
            tokenAPI.logIn(
                identity,
                linkToID = null,
                onSuccessHandler = { latch.countDown() },
                onErrorHandler = { fail("Should have succeeded: $it") },
            )
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        verify(exactly = 1) { mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `revokeOrLogout dedupes concurrent calls for the same appUserID into a single underlying call`() = runTest {
        val asyncDispatcher = Dispatcher(ThreadPoolExecutor(1, 2, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()))
        val tokenManager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        tokenManager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")
        val tokenAPI = tokenAPI(dispatcher = asyncDispatcher, tokenManager = tokenManager)
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            Thread.sleep(200)
            HTTPResult.createResult(200, "{}")
        }
        val latch = CountDownLatch(2)

        repeat(2) {
            tokenAPI.revokeOrLogout(
                "user",
                onSuccessHandler = { latch.countDown() },
                onErrorHandler = { fail("Should have succeeded: $it") },
            )
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        verify(exactly = 1) { mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // endregion

    // region token persistence on success

    @Test
    fun `a successful logIn saves the returned tokens`() = runTest {
        val tokenManager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        val tokenAPI = tokenAPI(dispatcher = SyncDispatcher(), tokenManager = tokenManager)
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(200, validLoginResponseBody("new-app-user-id"))
        var receivedResult: TokenLoginOperation.Result? = null

        tokenAPI.logIn(
            Identity.google("a-google-token".toByteArray()),
            linkToID = null,
            onSuccessHandler = { receivedResult = it },
            onErrorHandler = { fail("Should have succeeded: $it") },
        )

        assertThat(receivedResult?.appUserID).isEqualTo("new-app-user-id")
        assertThat(tokenManager.currentAccessToken("new-app-user-id")).isEqualTo(fakeAccessToken("new-app-user-id"))
        assertThat(tokenManager.currentRefreshToken("new-app-user-id")).isEqualTo("new-refresh")
        assertThat(tokenManager.currentIDToken("new-app-user-id")).isEqualTo("new-id")
    }

    // endregion

    // region error passthrough

    @Test
    fun `logIn passes the underlying error through, without saving any tokens`() = runTest {
        val tokenManager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        val tokenAPI = tokenAPI(dispatcher = SyncDispatcher(), tokenManager = tokenManager)
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(400, """{"code": 7226, "message": "bad request"}""")
        var receivedError: PurchasesError? = null

        tokenAPI.logIn(
            Identity.google("a-google-token".toByteArray()),
            linkToID = null,
            onSuccessHandler = { fail("Should have errored") },
            onErrorHandler = { receivedError = it },
        )

        assertThat(receivedError).isNotNull()
        assertThat(tokenManager.currentAccessToken("new-app-user-id")).isNull()
    }

    @Test
    fun `revokeOrLogout passes the underlying error through`() = runTest {
        val tokenManager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        tokenManager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")
        val tokenAPI = tokenAPI(dispatcher = SyncDispatcher(), tokenManager = tokenManager)
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(400, """{"code": 7226, "message": "bad request"}""")
        var receivedError: PurchasesError? = null

        tokenAPI.revokeOrLogout(
            "user",
            onSuccessHandler = { fail("Should have errored") },
            onErrorHandler = { receivedError = it },
        )

        assertThat(receivedError).isNotNull()
        assertThat(tokenManager.currentAccessToken("user")).isEqualTo("access")
    }

    // endregion

    private fun tokenAPI(dispatcher: Dispatcher, tokenManager: TokenManager? = null): TokenAPI {
        val manager = tokenManager ?: TokenManager(context, "test_api_key", enabled = true, scope = testScope())
        val backendHelper = BackendHelper("test_api_key", dispatcher, mockAppConfig, mockClient)
        return TokenAPI(mockAppConfig, dispatcher, mockClient, backendHelper, manager)
    }

    private fun validLoginResponseBody(appUserId: String): String =
        JSONObject()
            .put("access_token", fakeAccessToken(appUserId))
            .put("refresh_token", "new-refresh")
            .put("id_token", "new-id")
            .toString()

    private fun fakeAccessToken(appUserId: String): String {
        val payload = JSONObject().put("rc.app_user_id", appUserId)
        val header = base64Url("""{"alg":"none"}""")
        val body = base64Url(payload.toString())
        val signature = base64Url("")
        return "$header.$body.$signature"
    }

    private fun base64Url(value: String): String =
        android.util.Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
}
