package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.models.toHexString
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
    /** The stored SHA-256 of [data], as a 32-byte read-only view. */
    val checksum: ByteBuffer,
    /** A read-only, zero-copy view over this element's bytes. */
    val data: ByteBuffer,
    /** The element header's reserved u32 (currently always 0; reserved for content-types). */
    val reserved: Int = 0,
) {
    /**
     * Computes the SHA-256 of [data] and compares it against the stored [checksum].
     *
     * This reads directly off the backing buffer (via [ByteBuffer.duplicate]) so neither [data]
     * nor [checksum] is consumed, and [data] is not copied into an intermediate array.
     */
    fun isChecksumValid(): Boolean {
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
        digest.update(data.duplicate())
        val computed = digest.digest()

        val expected = checksum.duplicate()
        if (expected.remaining() != computed.size) return false
        return computed.all { it == expected.get() }
    }

    /** The stored [checksum] as a lowercase hex string, matching the backend's `checksum.hex()`. */
    fun checksumHex(): String {
        val view = checksum.duplicate()
        val bytes = ByteArray(view.remaining())
        view.get(bytes)
        return bytes.toHexString()
    }

    private companion object {
        private const val SHA_256_ALGORITHM = "SHA-256"
    }
}
