package com.revenuecat.purchases.common.networking

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * A single element within an [RCContainer].
 *
 * Both [checksum] and [data] are zero-copy, read-only views over the container's backing buffer:
 * no element bytes are copied or hashed during parsing. [data] holds the **on-wire** bytes, which
 * may be compressed per [codec]; [decode] yields the uncompressed content. The [checksum] always
 * covers the uncompressed bytes, so callers that need integrity verification opt in via
 * [isChecksumValid] (which decodes first).
 */
internal class RCElement(
    /** The stored SHA-256 of the **uncompressed** content truncated to 192 bits, as a 24-byte read-only view. */
    val checksum: ByteBuffer,
    /** A read-only, zero-copy view over this element's on-wire (possibly compressed) bytes. */
    val data: ByteBuffer,
    /** The element header's reserved u32 (its low byte is the [codec]; upper bytes reserved). */
    val reserved: Long = 0,
    /** The content-encoding codec id (see [RCContentEncoding]); 0 ([RCContentEncoding.NONE]) when uncompressed. */
    val codec: Int = RCContentEncoding.NONE.id,
) {
    /**
     * The uncompressed element content. [RCContentEncoding.NONE] returns the zero-copy [data] view; a
     * compressed codec inflates into a fresh buffer. Throws [RCContainerFormatException] for an
     * unsupported codec or a corrupt compressed stream.
     */
    fun decode(): ByteBuffer = RCContentEncoding.decode(data, codec)

    /**
     * Decodes the element and checks its SHA-256 (truncated to the stored [checksum]'s length) against
     * [checksum]. The backend stores `sha256(uncompressed)` truncated to its leftmost [checksum] bytes
     * (192 bits). An unsupported codec or corrupt stream makes [decode] throw, which is treated as a
     * failed verification (returns `false`).
     */
    @Suppress("SwallowedException") // An undecodable element simply fails verification.
    fun isChecksumValid(): Boolean = try {
        matchesChecksum(decode())
    } catch (e: RCContainerFormatException) {
        false
    }

    /**
     * Compares [content]'s SHA-256 (truncated to the stored [checksum]'s length) against [checksum].
     * Lets callers that already decoded the bytes verify them without inflating twice. Neither
     * [content] nor [checksum] is consumed (both are read via duplicates).
     */
    fun matchesChecksum(content: ByteBuffer): Boolean {
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
        // rewind so we hash the full content even if a caller has already read from the shared view.
        digest.update(content.duplicate().apply { rewind() })
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

    /** A copy of this element's [data] bytes (the payload exactly as received). */
    fun dataBytes(): ByteArray {
        val view = data.duplicate().apply { rewind() }
        val bytes = ByteArray(view.remaining())
        view.get(bytes)
        return bytes
    }

    private companion object {
        private const val SHA_256_ALGORITHM = "SHA-256"
    }
}
