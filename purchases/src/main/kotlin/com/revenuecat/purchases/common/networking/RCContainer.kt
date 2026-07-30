package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.common.warnLog
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A parsed RC Container Format v1 payload, providing zero-copy access to its fields.
 *
 * Layout (all multi-byte integers little-endian):
 * ```
 * Header (8 bytes): magic byte[2]="RC" | version u8 | flags u8 | reserved byte[4]
 * Element:          checksum byte[24]  | element_size u32 | reserved u32 | element[element_size] | pad→8
 * ```
 * Elements repeat until the backing buffer is exhausted (the format stores no count). Element 0 is
 * always the config; the remaining elements are content-addressed by checksum.
 *
 * The low byte of each element's `reserved` u32 is its content-encoding codec ([RCContentEncoding]):
 * `element_size` is then the **on-wire (compressed)** length, while the checksum covers the
 * **uncompressed** bytes. Header `flags` are parsed and ignored (forward-compatible).
 *
 * This class turns that raw form into usable content. [config] (always needed) is decoded and verified up
 * front during [parse]. The content blobs — often large and mostly unwanted on a given sync — stay compressed
 * in [contentElements]; a consumer iterates those and decodes (via [RCElement.decode], which verifies) only
 * the ones it wants, one at a time, so the whole payload is never held uncompressed at once. Parsing of the
 * content blobs stays zero-copy: [contentElements] are views over the backing buffer.
 */
internal class RCContainer private constructor(
    /** Format version from the header (always [SUPPORTED_VERSION] for a successful parse). */
    val version: Int,
    /** Reserved header flags (compression/encryption/etc.); currently unused. */
    val flags: Int,
    /**
     * The decoded, verified config JSON bytes (element 0). The backend may GZIP the config once it crosses a
     * size threshold; [parse] decodes it transparently, so callers get the uncompressed content regardless of
     * the on-wire codec. Its bytes are hashed against the config element's checksum during [parse] (a mismatch,
     * unsupported codec, or corrupt stream fails the parse); authentication remains the response signature over
     * these bytes (in `HTTPClient`), with the checksum only guarding against accidental corruption.
     */
    val config: ByteArray,
    /**
     * Elements 1..n in wire order — the raw, on-wire, zero-copy content blobs. Decode the ones you want via
     * [RCElement.decode] (which verifies) one at a time; decoding them all at once would hold the whole
     * uncompressed payload in memory, and inline blobs can be large.
     */
    val contentElements: List<RCElement>,
) {

    companion object {
        private const val MAGIC_R = 'R'.code.toByte()
        private const val MAGIC_C = 'C'.code.toByte()
        private const val SUPPORTED_VERSION = 1
        private const val HEADER_RESERVED_SIZE = 4
        private const val HEADER_FIXED_SIZE = 8
        private const val ALIGNMENT = 8
        private const val CHECKSUM_SIZE = 24
        private const val UINT32_SIZE = 4
        private const val ELEM_RESERVED_SIZE = 4
        private const val ELEMENT_HEADER_SIZE = CHECKSUM_SIZE + UINT32_SIZE + ELEM_RESERVED_SIZE
        private const val UINT32_MASK = 0xFFFFFFFFL

        // The low byte of the element reserved u32 is the codec; the upper 24 bits are reserved/ignored.
        private const val RESERVED_UPPER_BITS_MASK = 0xFFFFFF00L

        // Radix for rendering unknown flag/reserved values as hex in warning logs.
        private const val HEX_RADIX = 16

        fun parse(bytes: ByteArray): RCContainer = parse(ByteBuffer.wrap(bytes).asReadOnlyBuffer())

        /**
         * Parses [buffer] from its current position. The buffer is consumed up to the end of the
         * last element. A defensive read-only duplicate is taken so the caller's position/limit
         * are not modified. The config element (element 0) is decoded and verified here, into [config].
         *
         * @throws RCContainerFormatException if the bytes are not a valid RC Container Format v1 payload, or
         *   if the config element cannot be decoded/verified.
         */
        fun parse(buffer: ByteBuffer): RCContainer {
            // slice() (not duplicate()) so the working buffer's position 0 coincides with the
            // container start. Alignment padding is relative to the container start, so parsing from
            // a non-8-aligned caller position must not leak the caller's absolute offset into the
            // position()-based padding math. slice() is a view, so the caller's position is untouched.
            val source = buffer.slice().asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
            source.require(HEADER_FIXED_SIZE) {
                "Buffer too small for header: need at least $HEADER_FIXED_SIZE bytes, " +
                    "got ${source.remaining()}."
            }

            val (version, flags) = source.readAndValidateHeaderMeta()

            val parsed = mutableListOf<RCElement>()
            while (source.hasRemaining()) {
                source.require(ELEMENT_HEADER_SIZE) {
                    "Truncated element header: need $ELEMENT_HEADER_SIZE bytes, " +
                        "got ${source.remaining()}."
                }
                val checksum = ByteArray(CHECKSUM_SIZE).also { source.get(it) }
                val size = source.readUnsignedInt()
                val reserved = source.readUnsignedInt()
                // The codec id lives in the low byte of the reserved u32; upper bytes stay reserved.
                val codec = (reserved and BYTE_MASK.toLong()).toInt()
                val reservedUpperBits = reserved and RESERVED_UPPER_BITS_MASK
                if (reservedUpperBits != 0L) {
                    warnLog {
                        "RC element reserved bits non-zero (0x${reservedUpperBits.toString(HEX_RADIX)}); " +
                            "ignoring unknown reserved bits."
                    }
                }
                val data = source.sliceBytes(size, "element")
                source.alignTo(ALIGNMENT)
                parsed.add(RCElement(checksum = checksum, data = data, codec = codec))
            }

            if (parsed.isEmpty()) {
                throw RCContainerFormatException("Missing config element (element 0).")
            }

            return RCContainer(
                version = version,
                flags = flags,
                // element 0 is the config (decoded and verified up front, since it is always needed);
                // content elements are 1..n and stay compressed until consumed.
                config = parsed.first().decode(),
                contentElements = parsed.drop(1),
            )
        }

        private const val BYTE_MASK = 0xFF

        /** Reads and validates magic + version, skips the 4 reserved bytes, returns (version, flags). */
        private fun ByteBuffer.readAndValidateHeaderMeta(): Pair<Int, Int> {
            if (get() != MAGIC_R || get() != MAGIC_C) {
                throw RCContainerFormatException("Invalid magic bytes. Expected ASCII \"RC\".")
            }
            val version = get().toInt() and BYTE_MASK
            if (version != SUPPORTED_VERSION) {
                throw RCContainerFormatException("Unsupported version $version. Expected $SUPPORTED_VERSION.")
            }
            val flags = get().toInt() and BYTE_MASK
            if (flags != 0) {
                val flagsHex = flags.toString(HEX_RADIX)
                warnLog { "RC Container header flags non-zero (0x$flagsHex); ignoring unknown flags." }
            }
            position(position() + HEADER_RESERVED_SIZE)
            return version to flags
        }

        /** Throws [RCContainerFormatException] with [message] if fewer than [needed] bytes remain. */
        private fun ByteBuffer.require(needed: Int, message: () -> String) {
            if (remaining() < needed) throw RCContainerFormatException(message())
        }

        /** Reads a little-endian unsigned 32-bit integer as a [Long]. */
        private fun ByteBuffer.readUnsignedInt(): Long = int.toLong() and UINT32_MASK

        /**
         * Returns a read-only view of [size] bytes starting at the current position, advancing
         * the position past them. Validates [size] against the remaining bytes.
         */
        private fun ByteBuffer.sliceBytes(size: Long, fieldName: String): ByteBuffer {
            if (size < 0 || size > remaining()) {
                throw RCContainerFormatException(
                    "Declared $fieldName size $size exceeds remaining ${remaining()} bytes.",
                )
            }
            val sizeInt = size.toInt()
            val end = position() + sizeInt
            val view = duplicate().apply {
                limit(end)
            }.slice().asReadOnlyBuffer()
            position(end)
            return view
        }

        /** Advances the position to the next multiple of [alignment] (no-op if already aligned). */
        private fun ByteBuffer.alignTo(alignment: Int) {
            val remainder = position() % alignment
            if (remainder == 0) return
            val padding = alignment - remainder
            if (padding > remaining()) {
                // The final element may carry no trailing padding, so running out of bytes while
                // skipping alignment padding is a valid end-of-buffer, not a truncation error.
                position(limit())
            } else {
                position(position() + padding)
            }
        }
    }
}
