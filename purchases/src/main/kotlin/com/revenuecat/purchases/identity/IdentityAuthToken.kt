@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.toHexString
import java.security.MessageDigest

/**
 * An external identity credential supplied to IAM login, wrapping the raw payload bytes handed to
 * the SDK by the identity provider's own SDK (e.g. a raw OIDC/Google/Firebase ID token, or a Sign
 * in with Apple identity token). This is never validated locally -- it's still an "unvalidated,
 * quick-and-dirty" credential-to-be-exchanged (mirroring [com.revenuecat.purchases.common.JWT]'s
 * own no-signature-verification stance); the backend is what actually verifies it during
 * `/auth/login`.
 *
 * [Anonymous] is a real case, not the absence of one -- it's what the SDK uses for its silent
 * bootstrap login before any external identity provider is ever involved.
 */
internal sealed class IdentityAuthToken {

    /** Which external identity provider (or none, for [Anonymous]) this token came from. */
    abstract val authenticationMethod: IdentitySource

    /** The bytes that make this specific identity unique, for [cacheIdentifier] to hash. */
    protected abstract val payloadForCacheIdentifier: ByteArray

    /**
     * `true` unless this case wraps an empty payload. [Anonymous] has no payload to validate and
     * is always valid.
     */
    abstract fun validate(): Boolean

    /**
     * A stable, hash-based identifier for this specific identity credential: the same
     * `(authenticationMethod, payload)` pair always produces the same identifier, and different
     * payloads -- including across different [authenticationMethod]s, since the method is mixed
     * into the hash -- produce different ones. This lets callers key a cache or de-duplicate
     * concurrent identical requests (e.g. `TokenAPI.logIn`'s in-flight-call de-duplication) off an
     * opaque string rather than the raw token bytes themselves.
     *
     * Local-only value: never sent to the backend, and under no obligation to match whatever
     * equivalent value `purchases-ios` computes for the same credential.
     */
    val cacheIdentifier: String
        get() {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(authenticationMethod.rawValue.toByteArray())
            digest.update(payloadForCacheIdentifier)
            return digest.digest().toHexString()
        }

    /** The SDK's default identity before any external sign-in has happened. */
    object Anonymous : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.ANONYMOUS
        override val payloadForCacheIdentifier: ByteArray = ByteArray(0)
        override fun validate(): Boolean = true
        override fun toString(): String = "IdentityAuthToken.Anonymous"
    }

    data class Oidc(val identityToken: ByteArray) : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.OIDC
        override val payloadForCacheIdentifier: ByteArray get() = identityToken
        override fun validate(): Boolean = identityToken.isNotEmpty()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Oidc
            return identityToken.contentEquals(other.identityToken)
        }

        override fun hashCode(): Int = identityToken.contentHashCode()

        override fun toString(): String =
            "IdentityAuthToken.Oidc(identityToken=<${identityToken.size} bytes>)"
    }

    data class Google(val identityToken: ByteArray) : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.GOOGLE
        override val payloadForCacheIdentifier: ByteArray get() = identityToken
        override fun validate(): Boolean = identityToken.isNotEmpty()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Google
            return identityToken.contentEquals(other.identityToken)
        }

        override fun hashCode(): Int = identityToken.contentHashCode()

        override fun toString(): String =
            "IdentityAuthToken.Google(identityToken=<${identityToken.size} bytes>)"
    }

    data class SignInWithApple(val identityToken: ByteArray) : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.SIGN_IN_WITH_APPLE
        override val payloadForCacheIdentifier: ByteArray get() = identityToken
        override fun validate(): Boolean = identityToken.isNotEmpty()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SignInWithApple
            return identityToken.contentEquals(other.identityToken)
        }

        override fun hashCode(): Int = identityToken.contentHashCode()

        override fun toString(): String =
            "IdentityAuthToken.SignInWithApple(identityToken=<${identityToken.size} bytes>)"
    }

    data class Facebook(
        val identityToken: ByteArray,
        val email: String?,
    ) : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.FACEBOOK
        override val payloadForCacheIdentifier: ByteArray
            get() = identityToken + (email?.toByteArray() ?: ByteArray(0))
        override fun validate(): Boolean = identityToken.isNotEmpty()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Facebook
            if (!identityToken.contentEquals(other.identityToken)) return false
            return email == other.email
        }

        override fun hashCode(): Int = 31 * identityToken.contentHashCode() + (email?.hashCode() ?: 0)

        override fun toString(): String {
            val redactedEmail = if (email != null) "<redacted>" else "null"
            return "IdentityAuthToken.Facebook(identityToken=<${identityToken.size} bytes>, email=$redactedEmail)"
        }
    }

    data class Firebase(val identityToken: ByteArray) : IdentityAuthToken() {
        override val authenticationMethod: IdentitySource = IdentitySource.FIREBASE
        override val payloadForCacheIdentifier: ByteArray get() = identityToken
        override fun validate(): Boolean = identityToken.isNotEmpty()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Firebase
            return identityToken.contentEquals(other.identityToken)
        }

        override fun hashCode(): Int = identityToken.contentHashCode()

        override fun toString(): String =
            "IdentityAuthToken.Firebase(identityToken=<${identityToken.size} bytes>)"
    }
}
