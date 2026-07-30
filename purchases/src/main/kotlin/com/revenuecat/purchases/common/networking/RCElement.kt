package com.revenuecat.purchases.common.networking

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * A single element within an [RCContainer].
 *
 * [data] is a zero-copy, read-only view over the container's backing buffer — the potentially large payload
 * is never copied during parsing. It holds the **on-wire** bytes, which may be compressed per [codec]; [decode]
 * yields the uncompressed content as a fresh [ByteArray] (detached from the backing buffer), hashing it against
 * [checksum] before returning. Decoding is therefore inseparable from verification: there is no way to obtain
 * decoded bytes that don't match the element's content address. [checksum] is the small (24-byte) content
 * address, copied out during parsing since it is always needed (as a ref and for verification).
 */
internal class RCElement(
    /** The stored SHA-256 of the **uncompressed** content, truncated to 192 bits (24 bytes). */
    val checksum: ByteArray,
    /** A read-only, zero-copy view over this element's on-wire (possibly compressed) bytes. */
    val data: ByteBuffer,
    /** The content-encoding codec id (see [RCContentEncoding]); 0 ([RCContentEncoding.NONE]) when uncompressed. */
    val codec: Int = RCContentEncoding.NONE.id,
) {
    /**
     * The uncompressed, integrity-verified content as a fresh [ByteArray] detached from the backing buffer:
     * decodes per [codec], then hashes the result against [checksum] before returning it, so decoded bytes
     * always match the element's content address (and holding them doesn't pin the request buffer alive).
     * Throws [RCContainerFormatException] for an unsupported codec, a corrupt stream, or a checksum mismatch.
     */
    fun decode(): ByteArray {
        val decoded = RCContentEncoding.decode(data, codec)
        if (!matchesChecksum(decoded)) {
            throw RCContainerFormatException("RC element checksum verification failed.")
        }
        return decoded
    }

    /** The verification step baked into [decode]: does [content] hash to the stored (truncated) [checksum]? */
    private fun matchesChecksum(content: ByteArray): Boolean {
        val computed = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(content)
        // [checksum] is the SHA-256 truncated to 192 bits, so compare against that many leading digest bytes.
        return computed.copyOf(checksum.size).contentEquals(checksum)
    }

    /**
     * The [checksum] as a URL-safe, unpadded base64 string, matching the backend's ref encoding in the config
     * JSON / URLs (24 bytes -> 32 chars).
     */
    fun checksumBase64(): String =
        Base64.encodeToString(checksum, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private companion object {
        private const val SHA_256_ALGORITHM = "SHA-256"
    }
}
