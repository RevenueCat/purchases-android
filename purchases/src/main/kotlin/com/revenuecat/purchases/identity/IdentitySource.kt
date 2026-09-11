package com.revenuecat.purchases.identity

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * The kind of external identity provider a user authenticated with when signing in via IAM
 * login. Each case carries the raw string value the RevenueCat backend uses on the wire (e.g. as
 * the `amr` claim in an IAM-issued ID token, or the `method` field of a `/auth/login` request
 * body) — see [fromRawValue] for the inverse mapping.
 *
 * [ANONYMOUS] represents a user who hasn't authenticated with any external identity provider yet
 * (the default state before the SDK's first silent bootstrap login); it is still a real
 * [IdentitySource] value rather than the absence of one; a token's `amr` claim can list it
 * alongside other sources.
 */
@InternalRevenueCatAPI
public enum class IdentitySource(public val rawValue: String) {
    ANONYMOUS("anonymous"),
    OIDC("oidc"),
    GOOGLE("google"),
    SIGN_IN_WITH_APPLE("apple"),
    FACEBOOK("facebook"),
    FIREBASE("firebase"),
    ;

    public companion object {
        /**
         * Looks up the [IdentitySource] whose [rawValue] matches [value].
         *
         * @return the matching [IdentitySource], or `null` if [value] doesn't match any known
         *   source (e.g. a newer wire value this SDK version doesn't recognize yet).
         */
        public fun fromRawValue(value: String): IdentitySource? =
            entries.firstOrNull { it.rawValue == value }
    }
}
