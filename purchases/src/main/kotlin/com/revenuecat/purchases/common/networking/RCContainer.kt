package com.revenuecat.purchases.common.networking

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
 * always the [config]; the remaining elements are content-addressed by checksum.
 *
 * The low byte of each element's `reserved` u32 is its content-encoding codec ([RCContentEncoding]):
 * `element_size` is then the **on-wire (compressed)** length, while the checksum covers the
 * **uncompressed** bytes. Header `flags` are parsed and ignored (forward-compatible).
 *
 * [config] and each [RCElement] expose read-only views that share the backing buffer, so parsing
 * copies no field bytes. The content-addressed [elements] map is built lazily: only its base64 keys
 * allocate, and only on first access. Element bodies are always zero-copy views.
 */
internal class RCContainer private constructor(
    /** Format version from the header (always [SUPPORTED_VERSION] for a successful parse). */
    val version: Int,
    /** Reserved header flags (compression/encryption/etc.); currently unused. */
    val flags: Int,
    /** Element 0: the config. [RCElement.data] is a read-only, zero-copy view over the config bytes. */
    val config: RCElement,
    /** Elements 1..n in order. The zero-copy iteration path; no checksum keys are materialized. */
    val contentElements: List<RCElement>,
) {
    /**
     * The non-config elements addressed by their URL-safe base64 checksum (matching the backend's
     * ref encoding in the config JSON / URLs). Built lazily from [contentElements]; identical
     * payloads collapse to one entry (last wins). Element bodies remain the same zero-copy views as
     * in [contentElements].
     */
    val elements: Map<String, RCElement> by lazy { contentElements.associateBy { it.checksumBase64() } }

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

        fun parse(bytes: ByteArray): RCContainer = parse(ByteBuffer.wrap(bytes).asReadOnlyBuffer())

        /**
         * Parses [buffer] from its current position. The buffer is consumed up to the end of the
         * last element. A defensive read-only duplicate is taken so the caller's position/limit
         * are not modified.
         *
         * @throws RCContainerFormatException if the bytes are not a valid RC Container Format v1 payload.
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
                val checksum = source.sliceBytes(CHECKSUM_SIZE.toLong(), "checksum")
                val size = source.readUnsignedInt()
                val reserved = source.readUnsignedInt()
                // The codec id lives in the low byte of the reserved u32; upper bytes stay reserved.
                val codec = (reserved and BYTE_MASK.toLong()).toInt()
                val data = source.sliceBytes(size, "element")
                source.alignTo(ALIGNMENT)
                parsed.add(RCElement(checksum = checksum, data = data, reserved = reserved, codec = codec))
            }

            if (parsed.isEmpty()) {
                throw RCContainerFormatException("Missing config element (element 0).")
            }

            return RCContainer(
                version = version,
                flags = flags,
                config = parsed.first(),
                // element 0 is the config; content elements are 1..n
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
