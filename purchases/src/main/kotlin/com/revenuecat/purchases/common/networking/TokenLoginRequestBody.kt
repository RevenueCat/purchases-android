@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.identity.IdentityAuthToken
import com.revenuecat.purchases.identity.IdentitySource
import com.revenuecat.purchases.utils.filterNotNullValues

/**
 * The `/auth/login` request body shapes, one per [IdentityAuthToken] case. Built by hand as a plain
 * `Map<String, Any?>` (via [toMap]), matching this SDK's existing hand-built request-body convention
 * (see `Backend.logIn`'s `postFieldsToSign`) rather than `kotlinx.serialization` -- there's no JSON model
 * to decode on this side, only a map to send over the wire.
 *
 * [SCOPE]'s value is a placeholder: nothing in the IAM plan or the rest of this codebase pins down what
 * `/auth/login` actually expects for a `scope` field, so this is a best-effort default flagged here for
 * confirmation against the real backend contract, the same way the phase 5 prompt's own ground truth
 * flagged step 20's header-bridging question rather than silently guessing at it.
 */
internal sealed class TokenLoginRequestBody {

    abstract val method: String
    abstract val scope: String

    /**
     * This body's wire representation. Any `null` optional field (see [StandardBody.linkToID],
     * [FacebookBody.email], [FacebookBody.linkToID]) is omitted entirely rather than sent as an explicit
     * JSON `null` -- [MapConverter]/`org.json.JSONObject`'s map constructor does the latter by default,
     * which is why every case below runs its map through [filterNotNullValues] before returning it.
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
     * Every external identity provider except Facebook (oidc/google/apple/firebase), which alone also
     * carries an [email][FacebookBody.email] -- see [FacebookBody].
     *
     * @param linkToID the current (about-to-be-superseded) app user id to link this new identity's
     *   history to, or `null` for a login that shouldn't link anything (e.g. the current identity isn't
     *   anonymous, or there's nothing to link). Deciding whether a login should link is the caller's job
     *   (see `TokenLoginOperation`) -- this class only knows how to shape whatever value it's handed.
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

    /**
     * Facebook Login's identity credential, which alone among the supported providers may also carry an
     * [email] (only when the app requested and was granted Facebook's `email` permission -- never
     * guaranteed, hence nullable here too).
     */
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

        /**
         * The right [TokenLoginRequestBody] shape for [identity]'s underlying [IdentityAuthToken] case,
         * threading [linkToID] through to every case except [IdentityAuthToken.Anonymous] -- an anonymous
         * bootstrap login has no prior identity of its own to link.
         */
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
