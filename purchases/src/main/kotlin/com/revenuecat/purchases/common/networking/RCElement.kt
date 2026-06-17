package com.revenuecat.purchases.common.networking

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * A single element within an [RCContainer].
 *
 * Both [checksum] and [data] are zero-copy, read-only views over the container's backing buffer:
 * no element bytes are copied or hashed during parsing. Callers that need integrity verification
 * opt in explicitly via [isChecksumValid].
 */
internal class RCElement(
    /** The stored SHA-256 of [data] truncated to 192 bits, as a 24-byte read-only view. */
    val checksum: ByteBuffer,
    /** A read-only, zero-copy view over this element's bytes. */
    val data: ByteBuffer,
    /** The element header's reserved u32 (currently always 0; reserved for content-types). */
    val reserved: Long = 0,
) {
    /**
     * Computes the SHA-256 of [data], truncates it to the stored [checksum]'s length, and compares.
     *
     * The backend stores `sha256(data)` truncated to its leftmost [checksum] bytes (192 bits), so
     * we compare only that many leading bytes of the computed digest. This reads directly off the
     * backing buffer (via [ByteBuffer.duplicate]) so neither [data] nor [checksum] is consumed, and
     * [data] is not copied into an intermediate array.
     */
    fun isChecksumValid(): Boolean {
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
        // rewind so we hash the full element even if a caller has already read from the shared view.
        digest.update(data.duplicate().apply { rewind() })
        val computed = digest.digest()

        val expected = checksum.duplicate().apply { rewind() }
        val length = expected.remaining()
        return (0 until length).all { computed[it] == expected.get() }
    }

    /**
     * The stored [checksum] as a URL-safe, unpadded base64 string, matching the backend's ref
     * encoding in the config JSON / URLs (24 bytes -> 32 chars).
     */
    fun checksumBase64(): String {
        val view = checksum.duplicate().apply { rewind() }
        val bytes = ByteArray(view.remaining())
        view.get(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private companion object {
        private const val SHA_256_ALGORITHM = "SHA-256"
    }
}
