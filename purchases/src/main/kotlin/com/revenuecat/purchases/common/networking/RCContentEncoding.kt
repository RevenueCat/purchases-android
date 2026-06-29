package com.revenuecat.purchases.common.networking

import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream

/**
 * The per-element content-encoding codec carried in an [RCElement]'s header.
 *
 * The backend compresses each element body independently and stores the codec id in one of the element
 * header's reserved bytes. `element_size` is the **on-wire (compressed)** length, while the element checksum
 * always covers the **uncompressed** bytes, so a blob's content address (its ref) is stable across codec/level
 * changes: callers must [decode] before verifying the checksum or consuming the content.
 *
 * Android intentionally supports only [NONE] and [GZIP] (no extra dependencies) and advertises this with an
 * `Accept-Encoding` request header so the server never selects a codec it cannot decode. [BROTLI]/[ZSTD] and
 * unknown ids are surfaced as a decode failure ([RCContainerFormatException]) and handled gracefully upstream.
 */
@Suppress("MagicNumber")
internal enum class RCContentEncoding(val id: Int) {
    NONE(0),
    GZIP(1),
    BROTLI(2),
    ZSTD(3),
    ;

    companion object {
        fun fromId(id: Int): RCContentEncoding? = values().firstOrNull { it.id == id }

        /**
         * Returns the uncompressed bytes of [source] for the given [codecId].
         *
         * [NONE] returns [source] unchanged (zero-copy). [GZIP] inflates into a fresh read-only buffer. Any
         * other codec (including the known-but-unsupported [BROTLI]/[ZSTD]) or a corrupt gzip stream throws
         * [RCContainerFormatException]; callers treat that as a verification/decode failure.
         */
        fun decode(source: ByteBuffer, codecId: Int): ByteBuffer = when (fromId(codecId)) {
            NONE -> source
            GZIP -> ByteBuffer.wrap(gunzip(source)).asReadOnlyBuffer()
            BROTLI, ZSTD, null ->
                throw RCContainerFormatException("Unsupported content encoding id $codecId.")
        }

        private fun gunzip(source: ByteBuffer): ByteArray {
            val compressed = source.toByteArray()
            return try {
                GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
            } catch (e: IOException) {
                throw RCContainerFormatException("Failed to gzip-decode element.", e)
            }
        }

        /** Copies the remaining bytes of a duplicate, leaving [this]'s position untouched. */
        private fun ByteBuffer.toByteArray(): ByteArray {
            val view = duplicate().apply { rewind() }
            val bytes = ByteArray(view.remaining())
            view.get(bytes)
            return bytes
        }
    }
}
