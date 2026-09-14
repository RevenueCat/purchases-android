@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * An external identity credential to exchange for a RevenueCat session via IAM login (see
 * `Authentication.logIn`). Construct one with the factory matching the identity provider the app
 * already authenticated the user with -- [signInWithApple], [oidc], [google], [firebase], or
 * [facebook] -- there is no public constructor.
 *
 * This is a thin façade: the actual credential payload lives in [IdentityAuthToken], which stays
 * internal because its cases carry raw token bytes and aren't meaningful to app code beyond
 * "which provider, and here's the opaque token."
 */
@InternalRevenueCatAPI
public class Identity internal constructor(internal val authToken: IdentityAuthToken) {

    /** Which external identity provider this credential came from. */
    public val identitySource: IdentitySource
        get() = authToken.authenticationMethod

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identity) return false
        return authToken == other.authToken
    }

    override fun hashCode(): Int = authToken.hashCode()

    override fun toString(): String = "Identity($authToken)"

    public companion object {

        /**
         * An identity credential from a Sign in with Apple identity token.
         *
         * @param identityToken the raw identity token produced by Apple's sign-in SDK.
         */
        public fun signInWithApple(identityToken: ByteArray): Identity =
            Identity(IdentityAuthToken.SignInWithApple(identityToken))

        /**
         * An identity credential from a generic OIDC identity token.
         *
         * @param identityToken the raw ID token produced by the OIDC provider.
         */
        public fun oidc(identityToken: ByteArray): Identity =
            Identity(IdentityAuthToken.Oidc(identityToken))

        /**
         * An identity credential from a Google Sign-In identity token.
         *
         * @param identityToken the raw ID token produced by Google's sign-in SDK.
         */
        public fun google(identityToken: ByteArray): Identity =
            Identity(IdentityAuthToken.Google(identityToken))

        /**
         * An identity credential from a Firebase Authentication identity token.
         *
         * @param identityToken the raw ID token produced by Firebase Auth.
         */
        public fun firebase(identityToken: ByteArray): Identity =
            Identity(IdentityAuthToken.Firebase(identityToken))

        /**
         * An identity credential from a Facebook Login identity token.
         *
         * @param identityToken the raw identity token produced by the Facebook Login SDK.
         * @param email the user's email, if the app requested and was granted the `email`
         *   permission during Facebook Login. Optional -- Facebook Login doesn't guarantee it.
         */
        public fun facebook(identityToken: ByteArray, email: String?): Identity =
            Identity(IdentityAuthToken.Facebook(identityToken, email))

        /**
         * The SDK's own identity before any external sign-in has happened, used internally to
         * drive the silent bootstrap login (see `IdentityManager.needsIAMLogin`). Not exposed to
         * app code -- there's no scenario where an app should construct an anonymous [Identity]
         * itself.
         */
        internal val anonymous: Identity = Identity(IdentityAuthToken.Anonymous)
    }
}
