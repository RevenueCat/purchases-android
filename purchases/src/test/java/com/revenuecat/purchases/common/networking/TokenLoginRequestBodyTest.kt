@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.identity.IdentitySource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for the `/auth/login` request body shapes (IAM phase 5, step 15): the map each
 * [TokenLoginRequestBody] case produces, and [TokenLoginRequestBody.from]'s mapping from an [Identity]
 * to the right case.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenLoginRequestBodyTest {

    // region AnonymousBody

    @Test
    fun `AnonymousBody has the anonymous method and no other fields`() {
        val body = TokenLoginRequestBody.AnonymousBody()

        assertThat(body.toMap()).isEqualTo(
            mapOf("method" to IdentitySource.ANONYMOUS.rawValue, "scope" to TokenLoginRequestBody.SCOPE),
        )
    }

    // endregion

    // region StandardBody

    @Test
    fun `StandardBody carries method, scope, id_token and link_to_id`() {
        val body = TokenLoginRequestBody.StandardBody(
            method = IdentitySource.GOOGLE.rawValue,
            idToken = "a-google-id-token",
            linkToID = "old-anonymous-id",
        )

        assertThat(body.toMap()).isEqualTo(
            mapOf(
                "method" to IdentitySource.GOOGLE.rawValue,
                "scope" to TokenLoginRequestBody.SCOPE,
                "id_token" to "a-google-id-token",
                "link_to_id" to "old-anonymous-id",
            ),
        )
    }

    @Test
    fun `StandardBody omits link_to_id entirely when null, rather than sending an explicit null`() {
        val body = TokenLoginRequestBody.StandardBody(
            method = IdentitySource.OIDC.rawValue,
            idToken = "an-oidc-id-token",
            linkToID = null,
        )

        val map = body.toMap()
        assertThat(map).doesNotContainKey("link_to_id")
        assertThat(map).isEqualTo(
            mapOf(
                "method" to IdentitySource.OIDC.rawValue,
                "scope" to TokenLoginRequestBody.SCOPE,
                "id_token" to "an-oidc-id-token",
            ),
        )
    }

    // endregion

    // region FacebookBody

    @Test
    fun `FacebookBody carries method, scope, id_token, email and link_to_id`() {
        val body = TokenLoginRequestBody.FacebookBody(
            idToken = "a-facebook-id-token",
            email = "user@example.com",
            linkToID = "old-anonymous-id",
        )

        assertThat(body.toMap()).isEqualTo(
            mapOf(
                "method" to IdentitySource.FACEBOOK.rawValue,
                "scope" to TokenLoginRequestBody.SCOPE,
                "id_token" to "a-facebook-id-token",
                "email" to "user@example.com",
                "link_to_id" to "old-anonymous-id",
            ),
        )
    }

    @Test
    fun `FacebookBody omits email and link_to_id when null, rather than sending explicit nulls`() {
        val body = TokenLoginRequestBody.FacebookBody(
            idToken = "a-facebook-id-token",
            email = null,
            linkToID = null,
        )

        val map = body.toMap()
        assertThat(map).doesNotContainKey("email")
        assertThat(map).doesNotContainKey("link_to_id")
        assertThat(map).isEqualTo(
            mapOf(
                "method" to IdentitySource.FACEBOOK.rawValue,
                "scope" to TokenLoginRequestBody.SCOPE,
                "id_token" to "a-facebook-id-token",
            ),
        )
    }

    // endregion

    // region from(identity, linkToID)

    @Test
    fun `from builds AnonymousBody for the anonymous identity, ignoring linkToID`() {
        val body = TokenLoginRequestBody.from(Identity.anonymous, linkToID = "should-be-ignored")

        assertThat(body).isInstanceOf(TokenLoginRequestBody.AnonymousBody::class.java)
    }

    @Test
    fun `from builds a StandardBody with the right method for oidc, google, apple and firebase identities`() {
        val cases = listOf(
            Identity.oidc("oidc-token".toByteArray()) to IdentitySource.OIDC,
            Identity.google("google-token".toByteArray()) to IdentitySource.GOOGLE,
            Identity.signInWithApple("apple-token".toByteArray()) to IdentitySource.SIGN_IN_WITH_APPLE,
            Identity.firebase("firebase-token".toByteArray()) to IdentitySource.FIREBASE,
        )

        for ((identity, expectedSource) in cases) {
            val body = TokenLoginRequestBody.from(identity, linkToID = "linked-id")

            assertThat(body).isInstanceOf(TokenLoginRequestBody.StandardBody::class.java)
            val standard = body as TokenLoginRequestBody.StandardBody
            assertThat(standard.method)
                .withFailMessage { "Expected method ${expectedSource.rawValue} for $identity" }
                .isEqualTo(expectedSource.rawValue)
            assertThat(standard.linkToID).isEqualTo("linked-id")
        }
    }

    @Test
    fun `from builds a FacebookBody carrying the email and linkToID`() {
        val identity = Identity.facebook("facebook-token".toByteArray(), email = "user@example.com")

        val body = TokenLoginRequestBody.from(identity, linkToID = null)

        assertThat(body).isInstanceOf(TokenLoginRequestBody.FacebookBody::class.java)
        val facebook = body as TokenLoginRequestBody.FacebookBody
        assertThat(facebook.email).isEqualTo("user@example.com")
        assertThat(facebook.linkToID).isNull()
    }

    @Test
    fun `from round-trips the raw identity token bytes as a UTF-8 string`() {
        val identity = Identity.google("a-raw-google-id-token".toByteArray(Charsets.UTF_8))

        val body = TokenLoginRequestBody.from(identity, linkToID = null) as TokenLoginRequestBody.StandardBody

        assertThat(body.idToken).isEqualTo("a-raw-google-id-token")
    }

    // endregion
}
