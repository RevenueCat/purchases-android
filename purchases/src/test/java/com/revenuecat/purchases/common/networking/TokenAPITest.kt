@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.identity.Identity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenAPITest {

    private val baseURL = URL("https://api.revenuecat.com")
    private val apiKeyHeaders = mapOf("Authorization" to "Bearer api_key")

    private lateinit var tokenManager: TokenManager
    private lateinit var httpClient: HTTPClient
    private lateinit var dispatcher: DeferredDispatcher
    private lateinit var tokenAPI: TokenAPI

    private val bodySlot = slot<Map<String, Any?>>()

    @Before
    fun setUp() {
        tokenManager = mockk(relaxed = true) {
            every { enabled } returns true
            every { currentIDToken(any()) } returns null
            every { currentRefreshToken(any()) } returns null
        }
        httpClient = mockk()
        dispatcher = DeferredDispatcher()
        val appConfig = mockk<AppConfig> { every { baseURL } returns this@TokenAPITest.baseURL }
        val backendHelper = mockk<BackendHelper>(relaxed = true) {
            every { authenticationHeaders } returns apiKeyHeaders
            every { enqueue(any(), any(), any()) } answers { dispatcher.enqueue(firstArg(), Delay.NONE) }
        }
        tokenAPI = TokenAPI(tokenManager, httpClient, backendHelper, dispatcher, appConfig)
    }

    // region logIn

    @Test
    fun `logIn posts to auth login with the API key and saves the issued tokens`() {
        respond(Endpoint.TokenLogin, loginResponse(appUserID = "new-user"))
        var loggedInAs: String? = null

        tokenAPI.logIn("current-user", Identity.google("google-token".toByteArray()), { loggedInAs = it }, ::fail)
        dispatcher.runAll()

        assertThat(loggedInAs).isEqualTo("new-user")
        assertThat(bodySlot.captured["method"]).isEqualTo("google")
        verify { httpClient.performRequest(baseURL, Endpoint.TokenLogin, any(), null, apiKeyHeaders) }
        verify { tokenManager.saveTokens("new-user", accessToken(appUserID = "new-user"), "refresh", "id") }
    }

    @Test
    fun `logIn links to the current user's ID token`() {
        every { tokenManager.currentIDToken("current-user") } returns "current-id-token"
        respond(Endpoint.TokenLogin, loginResponse(appUserID = "new-user"))

        tokenAPI.logIn("current-user", Identity.google("google-token".toByteArray()), {}, ::fail)
        dispatcher.runAll()

        assertThat(bodySlot.captured["link_to_id"]).isEqualTo("current-id-token")
    }

    @Test
    fun `concurrent identical logIns share one request`() {
        respond(Endpoint.TokenLogin, loginResponse(appUserID = "new-user"))
        val results = mutableListOf<String>()
        val identity = Identity.google("google-token".toByteArray())

        tokenAPI.logIn("current-user", identity, { results.add(it) }, ::fail)
        tokenAPI.logIn("current-user", identity, { results.add(it) }, ::fail)
        dispatcher.runAll()

        assertThat(results).containsExactly("new-user", "new-user")
        verify(exactly = 1) { httpClient.performRequest(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `logIns with different identities are not shared`() {
        respond(Endpoint.TokenLogin, loginResponse(appUserID = "new-user"))

        tokenAPI.logIn("current-user", Identity.google("one".toByteArray()), {}, ::fail)
        tokenAPI.logIn("current-user", Identity.google("two".toByteArray()), {}, ::fail)
        dispatcher.runAll()

        verify(exactly = 2) { httpClient.performRequest(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `logIn passes a backend error through without saving tokens`() {
        respond(
            Endpoint.TokenLogin,
            HTTPResult.createResult(responseCode = RCHTTPStatusCodes.UNAUTHORIZED, payload = """{"code":7224}"""),
        )
        val errors = mutableListOf<PurchasesError>()

        tokenAPI.logIn("current-user", Identity.google("token".toByteArray()), { fail() }, { errors.add(it) })
        tokenAPI.logIn("current-user", Identity.google("token".toByteArray()), { fail() }, { errors.add(it) })
        dispatcher.runAll()

        assertThat(errors.map { it.code }).containsOnly(PurchasesErrorCode.InvalidCredentialsError).hasSize(2)
        verify(exactly = 0) { tokenManager.saveTokens(any(), any(), any(), any()) }
    }

    @Test
    fun `logIn passes a network error through`() {
        every { httpClient.performRequest(any(), any(), any(), any(), any()) } throws IOException("offline")
        var error: PurchasesError? = null

        tokenAPI.logIn("current-user", Identity.google("token".toByteArray()), { fail() }, { error = it })
        dispatcher.runAll()

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        verify(exactly = 0) { tokenManager.saveTokens(any(), any(), any(), any()) }
    }

    @Test
    fun `logIn with an invalid identity token fails without a network call`() {
        var error: PurchasesError? = null

        tokenAPI.logIn("current-user", Identity.google(ByteArray(0)), { fail() }, { error = it })

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
        assertThat(dispatcher.pending).isEmpty()
    }

    @Test
    fun `logIn fails without a network call when IAM is disabled`() {
        every { tokenManager.enabled } returns false
        var error: PurchasesError? = null

        tokenAPI.logIn("current-user", Identity.google("token".toByteArray()), { fail() }, { error = it })

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(dispatcher.pending).isEmpty()
    }

    // endregion

    // region revokeTokens

    @Test
    fun `revokeTokens revokes the refresh token, then clears local tokens`() {
        every { tokenManager.currentRefreshToken("user") } returns "refresh-token"
        respond(Endpoint.TokenLogout, HTTPResult.createResult())
        var succeeded = false

        tokenAPI.revokeTokens("user", { succeeded = true }, ::fail)
        dispatcher.runAll()

        assertThat(succeeded).isTrue()
        assertThat(bodySlot.captured)
            .isEqualTo(mapOf("token" to "refresh-token", "token_type_hint" to "refresh_token"))
        verify { httpClient.performRequest(baseURL, Endpoint.TokenLogout, any(), null, apiKeyHeaders) }
        verify { tokenManager.deleteTokens("user") }
    }

    @Test
    fun `revokeTokens without a refresh token clears local tokens without a network call`() {
        var succeeded = false

        tokenAPI.revokeTokens("user", { succeeded = true }, ::fail)

        assertThat(succeeded).isTrue()
        assertThat(dispatcher.pending).isEmpty()
        verify { tokenManager.deleteTokens("user") }
    }

    @Test
    fun `a failed revoke leaves local tokens in place`() {
        every { tokenManager.currentRefreshToken("user") } returns "refresh-token"
        respond(Endpoint.TokenLogout, HTTPResult.createResult(responseCode = RCHTTPStatusCodes.ERROR))
        var error: PurchasesError? = null

        tokenAPI.revokeTokens("user", { fail() }, { error = it })
        dispatcher.runAll()

        assertThat(error).isNotNull()
        verify(exactly = 0) { tokenManager.deleteTokens(any()) }
    }

    @Test
    fun `concurrent revokes for the same user share one request`() {
        every { tokenManager.currentRefreshToken("user") } returns "refresh-token"
        respond(Endpoint.TokenLogout, HTTPResult.createResult())
        var successes = 0

        tokenAPI.revokeTokens("user", { successes++ }, ::fail)
        tokenAPI.revokeTokens("user", { successes++ }, ::fail)
        dispatcher.runAll()

        assertThat(successes).isEqualTo(2)
        verify(exactly = 1) { httpClient.performRequest(any(), any(), any(), any(), any()) }
    }

    // endregion

    private fun respond(endpoint: Endpoint, result: HTTPResult) {
        every { httpClient.performRequest(any(), endpoint, capture(bodySlot), any(), any()) } returns result
    }

    private fun loginResponse(appUserID: String) = HTTPResult.createResult(
        payload = JSONObject()
            .put("access_token", accessToken(appUserID))
            .put("refresh_token", "refresh")
            .put("id_token", "id")
            .toString(),
    )

    private fun accessToken(appUserID: String): String =
        listOf("""{"alg":"none"}""", JSONObject().put("rc.app_user_id", appUserID).toString(), "")
            .joinToString(".") {
                Base64.encodeToString(it.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            }

    private fun fail(error: PurchasesError? = null): Nothing = throw AssertionError("Unexpected callback: $error")

    /** Holds enqueued calls until [runAll], so tests can make concurrent calls before any completes. */
    private class DeferredDispatcher : Dispatcher(mockk()) {
        val pending = mutableListOf<Runnable>()

        override fun enqueue(command: Runnable, delay: Delay) {
            pending.add(command)
        }

        override fun isClosed(): Boolean = false

        fun runAll() {
            while (pending.isNotEmpty()) pending.removeAt(0).run()
        }
    }
}
