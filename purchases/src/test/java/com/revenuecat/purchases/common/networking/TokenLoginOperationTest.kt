@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.identity.Identity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.URL

/**
 * Tests for [TokenLoginOperation] (IAM phase 5, step 16): the `/auth/login` networking primitive,
 * independent of `TokenAPI`'s dedup layer (step 19) and of `IdentityManager`'s linking decision (phase 6).
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenLoginOperationTest {

    private val baseURL = URL("http://mock-api-test.revenuecat.com/")
    private val authHeaders = mapOf("Authorization" to "Bearer test_api_key")
    private val dispatcher = SyncDispatcher()
    private var mockClient: HTTPClient = mockk()

    private var receivedResult: TokenLoginOperation.Result? = null
    private var receivedError: PurchasesError? = null

    private val onSuccessHandler: (TokenLoginOperation.Result) -> Unit = { receivedResult = it }
    private val onErrorHandler: (PurchasesError) -> Unit = { receivedError = it }

    // region invalid credential (no network call)

    @Test
    fun `an invalid (empty) identity credential short-circuits with no network call`() {
        logIn(Identity.google(ByteArray(0)))

        assertThat(receivedResult).isNull()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
        verify(exactly = 0) { mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // endregion

    // region success

    @Test
    fun `success with a valid JWT extracts the app user id and every token`() {
        mockResponse(
            responseCode = 200,
            body = JSONObject()
                .put("access_token", fakeAccessToken(appUserId = "new-app-user-id"))
                .put("refresh_token", "a-refresh-token")
                .put("id_token", "an-id-token")
                .toString(),
        )

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedError).isNull()
        assertThat(receivedResult?.appUserID).isEqualTo("new-app-user-id")
        assertThat(receivedResult?.tokens).isEqualTo(
            TokenManager.TokenSet(
                accessToken = fakeAccessToken(appUserId = "new-app-user-id"),
                refreshToken = "a-refresh-token",
                idToken = "an-id-token",
            ),
        )
    }

    @Test
    fun `logIn performs the right http call for an anonymous identity`() {
        mockResponse(
            responseCode = 200,
            body = JSONObject()
                .put("access_token", fakeAccessToken(appUserId = "anon-app-user-id"))
                .put("refresh_token", "refresh")
                .put("id_token", "id")
                .toString(),
        )

        logIn(Identity.anonymous, linkToID = null)

        verify(exactly = 1) {
            mockClient.performRequest(
                baseURL,
                Endpoint.TokenLogin,
                TokenLoginRequestBody.AnonymousBody().toMap(),
                null,
                authHeaders,
                any(),
                emptyList(),
                0,
            )
        }
    }

    // endregion

    // region malformed backend response

    @Test
    fun `a missing app_user_id claim is an internal error, not a crash`() {
        mockResponse(
            responseCode = 200,
            body = JSONObject()
                .put("access_token", fakeAccessToken(appUserId = null))
                .put("refresh_token", "a-refresh-token")
                .put("id_token", "an-id-token")
                .toString(),
        )

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedResult).isNull()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    @Test
    fun `an undecodable access token is an internal error, not a crash`() {
        mockResponse(
            responseCode = 200,
            body = JSONObject()
                .put("access_token", "not-a-jwt")
                .put("refresh_token", "a-refresh-token")
                .put("id_token", "an-id-token")
                .toString(),
        )

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedResult).isNull()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    @Test
    fun `a response missing a token field is an internal error, not a crash`() {
        mockResponse(
            responseCode = 200,
            body = JSONObject()
                .put("access_token", fakeAccessToken(appUserId = "new-app-user-id"))
                .put("id_token", "an-id-token")
                .toString(),
        )

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedResult).isNull()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    // endregion

    // region backend / network errors

    @Test
    fun `an unsuccessful http response is surfaced as an error`() {
        mockResponse(responseCode = 400, body = """{"code": 7226, "message": "bad request"}""")

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedResult).isNull()
        assertThat(receivedError).isNotNull()
    }

    @Test
    fun `a network failure is passed through as an error, not swallowed`() {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } throws IOException("no connection")

        logIn(Identity.google("a-google-id-token".toByteArray()))

        assertThat(receivedResult).isNull()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    // endregion

    private fun logIn(identity: Identity, linkToID: String? = null) {
        TokenLoginOperation.logIn(
            baseURL,
            mockClient,
            dispatcher,
            authHeaders,
            identity,
            linkToID,
            onSuccessHandler = onSuccessHandler,
            onErrorHandler = onErrorHandler,
        )
    }

    private fun mockResponse(responseCode: Int, body: String) {
        every {
            mockClient.performRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.createResult(responseCode, body)
    }

    /**
     * A syntactically-valid (unsigned, unverified) JWT carrying [appUserId] as its `rc.app_user_id` claim
     * -- or no such claim at all when [appUserId] is `null`.
     */
    private fun fakeAccessToken(appUserId: String?): String {
        val payload = JSONObject().apply {
            if (appUserId != null) put("rc.app_user_id", appUserId)
        }
        val header = base64Url("""{"alg":"none"}""")
        val body = base64Url(payload.toString())
        val signature = base64Url("")
        return "$header.$body.$signature"
    }

    private fun base64Url(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
