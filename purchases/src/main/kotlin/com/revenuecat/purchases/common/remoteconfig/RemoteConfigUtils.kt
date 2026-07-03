package com.revenuecat.purchases.common.remoteconfig

import android.util.Base64
import java.security.MessageDigest

/**
 * Shared helpers for remote-config blob refs. A ref is a content address: the blob's SHA-256 truncated to
 * [REF_HASH_BYTES] (192 bits), URL-safe base64 with no padding — a fixed 32-char, filename-safe string that
 * mirrors `RCElement.checksumBase64`, so inline and fetched blobs share the same ref shape.
 *
 * Centralizes the validate/create logic that [RemoteConfigBlobStore] and [RemoteConfigBlobFetcher] both need.
 */
internal object RemoteConfigUtils {
    private const val REF_HASH_BYTES = 24
    private const val SHA_256_ALGORITHM = "SHA-256"

    /** 24-byte hash -> 32 URL-safe base64 chars, unpadded. */
    private val REF_REGEX = Regex("^[A-Za-z0-9_-]{32}$")

    /** Whether [ref] matches the content-address ref shape; the guard that keeps a malformed ref filename-safe. */
    fun isValidRef(ref: String): Boolean = REF_REGEX.matches(ref)

    /**
     * The content-address ref of [bytes]: SHA-256 truncated to [REF_HASH_BYTES] (192 bits), URL-safe base64 with
     * no padding. Mirrors `RCElement.checksumBase64`, so inline and fetched blobs verify against the same ref shape.
     */
    fun contentAddressRef(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(bytes)
        val truncated = digest.copyOf(REF_HASH_BYTES)
        return Base64.encodeToString(truncated, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
