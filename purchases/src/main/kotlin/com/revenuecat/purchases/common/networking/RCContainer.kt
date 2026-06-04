package com.revenuecat.purchases.common.networking

import java.nio.ByteBuffer

/**
 * A parsed RC Container Format v1 payload, providing zero-copy access to its fields.
 *
 * Layout:
 * ```
 * Header:  magic byte[2]="RC" | version u8 | flags u8 | config_size u32 | config[config_size] | pad→8
 * Element: checksum byte[32]  | element_size u32 | element[element_size] | pad→8   (repeat until EOF)
 * ```
 * All `u32` fields are big-endian (network order). The number of elements is not stored; elements
 * are read until the backing buffer is exhausted.
 *
 * [config] and each [RCElement] expose read-only views that share the backing buffer, so parsing
 * does not copy field bytes.
 */
internal class RCContainer private constructor(
    /** Format version from the header (always [SUPPORTED_VERSION] for a successful parse). */
    val version: Int,
    /** Reserved header flags (compression/encryption/etc.); currently unused. */
    val flags: Int,
    /** A read-only, zero-copy view over the config JSON bytes. */
    val config: ByteBuffer,
    /** The container's elements, in order. */
    val elements: List<RCElement>,
) {
    companion object {
        private const val MAGIC_R = 'R'.code.toByte()
        private const val MAGIC_C = 'C'.code.toByte()
        private const val SUPPORTED_VERSION = 1
        private const val HEADER_FIXED_SIZE = 8
        private const val ALIGNMENT = 8
        private const val CHECKSUM_SIZE = 32
        private const val UINT32_SIZE = 4
        private const val ELEMENT_HEADER_SIZE = CHECKSUM_SIZE + UINT32_SIZE
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
            val source = buffer.duplicate().asReadOnlyBuffer()
            source.require(HEADER_FIXED_SIZE) {
                "Buffer too small for header: need at least $HEADER_FIXED_SIZE bytes, " +
                    "got ${source.remaining()}."
            }

            val (version, flags) = source.readAndValidateHeaderMeta()
            val config = source.sliceBytes(source.readUnsignedInt(), "config")
            source.alignTo(ALIGNMENT)

            val elements = mutableListOf<RCElement>()
            while (source.hasRemaining()) {
                source.require(ELEMENT_HEADER_SIZE) {
                    "Truncated element header: need $ELEMENT_HEADER_SIZE bytes, " +
                        "got ${source.remaining()}."
                }
                val checksum = source.sliceBytes(CHECKSUM_SIZE.toLong(), "checksum")
                val data = source.sliceBytes(source.readUnsignedInt(), "element")
                source.alignTo(ALIGNMENT)
                elements.add(RCElement(checksum = checksum, data = data))
            }

            return RCContainer(
                version = version,
                flags = flags,
                config = config,
                elements = elements,
            )
        }

        private const val BYTE_MASK = 0xFF

        /** Reads and validates magic + version, returning the (version, flags) pair. */
        private fun ByteBuffer.readAndValidateHeaderMeta(): Pair<Int, Int> {
            if (get() != MAGIC_R || get() != MAGIC_C) {
                throw RCContainerFormatException("Invalid magic bytes. Expected ASCII \"RC\".")
            }
            val version = get().toInt() and BYTE_MASK
            if (version != SUPPORTED_VERSION) {
                throw RCContainerFormatException("Unsupported version $version. Expected $SUPPORTED_VERSION.")
            }
            val flags = get().toInt() and BYTE_MASK
            return version to flags
        }

        /** Throws [RCContainerFormatException] with [message] if fewer than [needed] bytes remain. */
        private fun ByteBuffer.require(needed: Int, message: () -> String) {
            if (remaining() < needed) throw RCContainerFormatException(message())
        }

        /** Reads a big-endian unsigned 32-bit integer as a [Long]. */
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
                // Trailing alignment padding past the end of the buffer; nothing more to read.
                position(limit())
            } else {
                position(position() + padding)
            }
        }
    }
}
