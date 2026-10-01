@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.identity.IdentityAuthToken
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenOperationsTest {

    // region TokenLoginOperation.body

    @Test
    fun `anonymous body has only method and scope`() {
        val body = TokenLoginOperation.body(IdentityAuthToken.Anonymous, linkToID = "ignored")

        assertThat(body).isEqualTo(mapOf("method" to "anonymous", "scope" to "openid offline_access"))
    }

    @Test
    fun `standard bodies carry the identity token and link_to_id`() {
        val cases = mapOf(
            IdentityAuthToken.Oidc("oidc-token".toByteArray()) to "oidc",
            IdentityAuthToken.Google("google-token".toByteArray()) to "google",
            IdentityAuthToken.SignInWithApple("apple-token".toByteArray()) to "apple",
            IdentityAuthToken.Firebase("firebase-token".toByteArray()) to "firebase",
        )
        for ((token, method) in cases) {
            assertThat(TokenLoginOperation.body(token, linkToID = "current-id-token")).isEqualTo(
                mapOf(
                    "method" to method,
                    "scope" to "openid offline_access",
                    "id_token" to "$method-token",
                    "link_to_id" to "current-id-token",
                ),
            )
        }
    }

    @Test
    fun `a null link_to_id is omitted`() {
        val body = TokenLoginOperation.body(IdentityAuthToken.Google("token".toByteArray()), linkToID = null)

        assertThat(body).doesNotContainKey("link_to_id")
        assertThat(body["id_token"]).isEqualTo("token")
    }

    @Test
    fun `facebook body carries the email when present`() {
        val body = TokenLoginOperation.body(
            IdentityAuthToken.Facebook("fb-token".toByteArray(), email = "a@b.c"),
            linkToID = "current-id-token",
        )

        assertThat(body).isEqualTo(
            mapOf(
                "method" to "facebook",
                "scope" to "openid offline_access",
                "id_token" to "fb-token",
                "email" to "a@b.c",
                "link_to_id" to "current-id-token",
            ),
        )
    }

    @Test
    fun `facebook body omits a null email`() {
        val body = TokenLoginOperation.body(IdentityAuthToken.Facebook("fb-token".toByteArray(), email = null), null)

        assertThat(body).doesNotContainKey("email")
    }

    @Test
    fun `an identity token that isn't valid UTF-8 is sent as base64`() {
        val bytes = byteArrayOf(0xC3.toByte(), 0x28)

        val body = TokenLoginOperation.body(IdentityAuthToken.Oidc(bytes), linkToID = null)

        assertThat(body["id_token"]).isEqualTo(Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    // endregion

    // region TokenLoginOperation.handleResponse

    @Test
    fun `a successful response yields the tokens and the JWT's app user ID`() {
        val accessToken = fakeJWT(JSONObject().put("rc.app_user_id", "user-123"))
        val result = HTTPResult.createResult(payload = tokenResponse(accessToken, "refresh", "id").toString())

        val success = TokenLoginOperation.handleResponse(result) as TokenLoginOperation.Result.Success

        assertThat(success.tokens).isEqualTo(TokenManager.TokenSet(accessToken, "refresh", "id"))
        assertThat(success.appUserID).isEqualTo("user-123")
    }

    @Test
    fun `an access token without an app user ID claim is an unexpected response`() {
        val accessToken = fakeJWT(JSONObject().put("iss", "rc"))
        val result = HTTPResult.createResult(payload = tokenResponse(accessToken, "refresh", "id").toString())

        assertUnexpectedResponse(TokenLoginOperation.handleResponse(result))
    }

    @Test
    fun `an undecodable access token is an unexpected response`() {
        val result = HTTPResult.createResult(payload = tokenResponse("not-a-jwt", "refresh", "id").toString())

        assertUnexpectedResponse(TokenLoginOperation.handleResponse(result))
    }

    @Test
    fun `a response without an access token is an unexpected response`() {
        val result = HTTPResult.createResult(payload = JSONObject().put("refresh_token", "refresh").toString())

        assertUnexpectedResponse(TokenLoginOperation.handleResponse(result))
    }

    @Test
    fun `a backend error is passed through`() {
        val result = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.UNAUTHORIZED,
            payload = JSONObject().put("code", 7225).put("message", "Invalid token").toString(),
        )

        val error = (TokenLoginOperation.handleResponse(result) as TokenLoginOperation.Result.Error).error

        assertThat(error.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
    }

    // endregion

    // region TokenResponse

    @Test
    fun `refresh and ID tokens are optional`() {
        val tokens = TokenResponse.parse(JSONObject().put("access_token", "access"))

        assertThat(tokens).isEqualTo(TokenManager.TokenSet("access", refreshToken = null, idToken = null))
    }

    @Test
    fun `an empty access token is treated as missing`() {
        assertThat(TokenResponse.parse(JSONObject().put("access_token", ""))).isNull()
    }

    // endregion

    private fun assertUnexpectedResponse(result: TokenLoginOperation.Result) {
        assertThat(result).isInstanceOf(TokenLoginOperation.Result.Error::class.java)
        assertThat((result as TokenLoginOperation.Result.Error).error.code)
            .isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    private fun tokenResponse(accessToken: String, refreshToken: String, idToken: String) = JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken)
        .put("id_token", idToken)
        .put("scope", "openid offline_access")
        .put("expires_in", 3600)

    private fun fakeJWT(payload: JSONObject): String =
        listOf("""{"alg":"none"}""", payload.toString(), "").joinToString(".") {
            Base64.encodeToString(it.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
}
