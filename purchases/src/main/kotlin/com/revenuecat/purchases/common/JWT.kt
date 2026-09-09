package com.revenuecat.purchases.common

import android.util.Base64
import com.revenuecat.purchases.utils.optNullableString
import com.revenuecat.purchases.utils.toList
import org.json.JSONException
import org.json.JSONObject

/**
 * A quick-and-dirty, **unvalidated** parse of a JWT's payload claims.
 *
 * This deliberately does not verify the token's signature (or even look at its header contents)
 * — it exists only to read a handful of well-known claims off of a token the SDK already trusts
 * because it just received it directly from RevenueCat's backend or from an identity provider's
 * SDK. Treat [issuer], [appUserId], and [amr] as informational only.
 *
 * @param payload the decoded JSON payload (the JWT's second `.`-delimited segment)
 */
internal class JWT private constructor(private val payload: JSONObject) {

    /** The `iss` claim, if present. */
    val issuer: String?
        get() = payload.optNullableString(ISSUER_CLAIM)

    /** The `rc.app_user_id` claim, if present. */
    val appUserId: String?
        get() = payload.optNullableString(APP_USER_ID_CLAIM)

    /** The `amr` (Authentication Methods References) claim, if present. */
    val amr: List<String>?
        get() = payload.optJSONArray(AMR_CLAIM)?.toList()

    companion object {
        private const val EXPECTED_SEGMENT_COUNT = 3
        private const val PAYLOAD_SEGMENT_INDEX = 1

        private const val ISSUER_CLAIM = "iss"
        private const val APP_USER_ID_CLAIM = "rc.app_user_id"
        private const val AMR_CLAIM = "amr"

        /**
         * Parses [token] as a `header.payload.signature` JWT and returns its decoded payload
         * claims, or `null` if [token] isn't well-formed enough to read.
         *
         * A token with `alg: "none"` has an empty (but present) signature segment — that still
         * counts as 3 segments, and an empty segment decodes to an empty (but valid) byte array,
         * so it's handled the same as any other token. Note that [CharSequence.split] (the
         * `String`-vararg overload used here), unlike Java's regex-based `String.split`, does not
         * drop trailing empty strings, so an empty signature segment doesn't collapse the segment
         * count.
         *
         * All three segments must decode as valid base64url, even though only the payload's
         * claims are actually read — a token that isn't even structurally valid base64url isn't
         * a JWT worth trusting any part of.
         */
        fun decode(token: String): JWT? {
            val segments = token.split(".")
            if (segments.size != EXPECTED_SEGMENT_COUNT) return null

            val decodedSegments = segments.map { decodeBase64UrlSegment(it) ?: return null }

            val payload = try {
                JSONObject(decodedSegments[PAYLOAD_SEGMENT_INDEX])
            } catch (@Suppress("SwallowedException") e: JSONException) {
                return null
            }

            return JWT(payload)
        }

        private fun decodeBase64UrlSegment(segment: String): String? {
            return try {
                val bytes = Base64.decode(segment, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                String(bytes, Charsets.UTF_8)
            } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
                null
            }
        }
    }
}
