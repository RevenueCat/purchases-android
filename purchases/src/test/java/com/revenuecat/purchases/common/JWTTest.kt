package com.revenuecat.purchases.common

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class JWTTest {

    // region valid tokens

    @Test
    fun `decodes issuer, appUserId, and amr from a valid JWT`() {
        val token = buildToken(
            payload = JSONObject().apply {
                put("iss", "https://api.revenuecat.com")
                put("rc.app_user_id", "user-1234")
                put("amr", JSONArray(listOf("pwd", "otp")))
            },
        )

        val jwt = JWT.decode(token)

        assertThat(jwt).isNotNull
        assertThat(jwt!!.issuer).isEqualTo("https://api.revenuecat.com")
        assertThat(jwt.appUserId).isEqualTo("user-1234")
        assertThat(jwt.amr).isEqualTo(listOf("pwd", "otp"))
    }

    @Test
    fun `decodes a token with alg none and an empty signature segment`() {
        val payload = JSONObject().apply {
            put("iss", "https://api.revenuecat.com")
        }
        val header = base64UrlEncode("""{"alg":"none"}""")
        val encodedPayload = base64UrlEncode(payload.toString())
        val token = "$header.$encodedPayload."

        val jwt = JWT.decode(token)

        assertThat(jwt).isNotNull
        assertThat(jwt!!.issuer).isEqualTo("https://api.revenuecat.com")
    }

    @Test
    fun `missing claims are read as null`() {
        val token = buildToken(payload = JSONObject())

        val jwt = JWT.decode(token)

        assertThat(jwt).isNotNull
        assertThat(jwt!!.issuer).isNull()
        assertThat(jwt.appUserId).isNull()
        assertThat(jwt.amr).isNull()
    }

    // endregion

    // region malformed tokens

    @Test
    fun `returns null when the token does not have exactly 3 segments`() {
        assertThat(JWT.decode("only-one-segment")).isNull()
        assertThat(JWT.decode("two.segments")).isNull()
        assertThat(JWT.decode("way.too.many.segments")).isNull()
        assertThat(JWT.decode("")).isNull()
    }

    @Test
    fun `returns null when the header segment is invalid base64url`() {
        val encodedPayload = base64UrlEncode(JSONObject().toString())
        val token = "$invalidBase64Segment.$encodedPayload.$validSignature"

        assertThat(JWT.decode(token)).isNull()
    }

    @Test
    fun `returns null when the payload segment is invalid base64url`() {
        val header = base64UrlEncode("""{"alg":"none"}""")
        val token = "$header.$invalidBase64Segment.$validSignature"

        assertThat(JWT.decode(token)).isNull()
    }

    @Test
    fun `returns null when the signature segment is invalid base64url`() {
        // The signature's contents are never read, but the token as a whole still isn't
        // structurally valid if one of its segments isn't valid base64url.
        val header = base64UrlEncode("""{"alg":"none"}""")
        val encodedPayload = base64UrlEncode(JSONObject().toString())
        val token = "$header.$encodedPayload.$invalidBase64Segment"

        assertThat(JWT.decode(token)).isNull()
    }

    @Test
    fun `returns null when the payload is not a JSON object`() {
        val header = base64UrlEncode("""{"alg":"none"}""")
        val encodedPayload = base64UrlEncode("[1,2,3]")
        val token = "$header.$encodedPayload.$validSignature"

        assertThat(JWT.decode(token)).isNull()
    }

    @Test
    fun `returns null when the payload is not valid JSON at all`() {
        val header = base64UrlEncode("""{"alg":"none"}""")
        val encodedPayload = base64UrlEncode("not json")
        val token = "$header.$encodedPayload.$validSignature"

        assertThat(JWT.decode(token)).isNull()
    }

    // endregion

    // A fake but structurally valid (i.e. base64url-decodable) signature segment, for tests
    // that need the header/payload decode path to succeed regardless of the signature.
    private val validSignature = base64UrlEncode("signature")

    // A single character can never be valid base64 -- decoding requires at least 2 input
    // characters to produce 1 output byte -- so this is guaranteed to fail to decode
    // regardless of alphabet leniency toward any individual character.
    private val invalidBase64Segment = "!"

    private fun buildToken(
        payload: JSONObject,
        header: JSONObject = JSONObject().apply { put("alg", "HS256") },
    ): String {
        val encodedHeader = base64UrlEncode(header.toString())
        val encodedPayload = base64UrlEncode(payload.toString())
        return "$encodedHeader.$encodedPayload.$validSignature"
    }

    private fun base64UrlEncode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
