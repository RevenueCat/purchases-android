@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.identity.IdentityAuthToken
import com.revenuecat.purchases.identity.IdentitySource
import com.revenuecat.purchases.utils.filterNotNullValues

/**
 * The `/auth/login` request body shapes, one per [IdentityAuthToken] case. Built as a plain
 * `Map<String, Any?>` (via [toMap]), matching `Backend.logIn`'s hand-built convention.
 *
 * [SCOPE]'s value is a placeholder pending confirmation against the real `/auth/login` contract.
 */
internal sealed class TokenLoginRequestBody {

    abstract val method: String
    abstract val scope: String

    /**
     * This body's wire representation. `null` optional fields are omitted (via [filterNotNullValues])
     * rather than sent as an explicit JSON `null`.
     */
    abstract fun toMap(): Map<String, Any?>

    /** The SDK's own silent bootstrap login, before any external identity provider is involved. */
    data class AnonymousBody(
        override val method: String = IdentitySource.ANONYMOUS.rawValue,
        override val scope: String = SCOPE,
    ) : TokenLoginRequestBody() {
        override fun toMap(): Map<String, Any?> = mapOf(METHOD_KEY to method, SCOPE_KEY to scope)
    }

    /**
     * Every external identity provider except Facebook (see [FacebookBody]).
     *
     * @param linkToID the app user id to link this identity's history to, or `null` if nothing should
     *   link. Deciding whether to link is the caller's job (`TokenLoginOperation`).
     */
    data class StandardBody(
        override val method: String,
        val idToken: String,
        val linkToID: String?,
        override val scope: String = SCOPE,
    ) : TokenLoginRequestBody() {
        override fun toMap(): Map<String, Any?> = mapOf(
            METHOD_KEY to method,
            SCOPE_KEY to scope,
            ID_TOKEN_KEY to idToken,
            LINK_TO_ID_KEY to linkToID,
        ).filterNotNullValues()
    }

    /** Facebook Login's identity credential; [email] is present only when that permission was granted. */
    data class FacebookBody(
        val idToken: String,
        val email: String?,
        val linkToID: String?,
        override val scope: String = SCOPE,
    ) : TokenLoginRequestBody() {
        override val method: String = IdentitySource.FACEBOOK.rawValue
        override fun toMap(): Map<String, Any?> = mapOf(
            METHOD_KEY to method,
            SCOPE_KEY to scope,
            ID_TOKEN_KEY to idToken,
            EMAIL_KEY to email,
            LINK_TO_ID_KEY to linkToID,
        ).filterNotNullValues()
    }

    companion object {
        private const val METHOD_KEY = "method"
        private const val SCOPE_KEY = "scope"
        private const val ID_TOKEN_KEY = "id_token"
        private const val LINK_TO_ID_KEY = "link_to_id"
        private const val EMAIL_KEY = "email"

        // TODO(IAM): placeholder value pending confirmation against the real /auth/login contract.
        internal const val SCOPE = "identify"

        /** The right [TokenLoginRequestBody] shape for [identity]'s [IdentityAuthToken] case. */
        fun from(identity: Identity, linkToID: String?): TokenLoginRequestBody {
            return when (val token = identity.authToken) {
                is IdentityAuthToken.Anonymous -> AnonymousBody()
                is IdentityAuthToken.Facebook -> FacebookBody(
                    idToken = token.identityToken.toString(Charsets.UTF_8),
                    email = token.email,
                    linkToID = linkToID,
                )
                is IdentityAuthToken.Oidc -> StandardBody(
                    method = IdentitySource.OIDC.rawValue,
                    idToken = token.identityToken.toString(Charsets.UTF_8),
                    linkToID = linkToID,
                )
                is IdentityAuthToken.Google -> StandardBody(
                    method = IdentitySource.GOOGLE.rawValue,
                    idToken = token.identityToken.toString(Charsets.UTF_8),
                    linkToID = linkToID,
                )
                is IdentityAuthToken.SignInWithApple -> StandardBody(
                    method = IdentitySource.SIGN_IN_WITH_APPLE.rawValue,
                    idToken = token.identityToken.toString(Charsets.UTF_8),
                    linkToID = linkToID,
                )
                is IdentityAuthToken.Firebase -> StandardBody(
                    method = IdentitySource.FIREBASE.rawValue,
                    idToken = token.identityToken.toString(Charsets.UTF_8),
                    linkToID = linkToID,
                )
            }
        }
    }
}
