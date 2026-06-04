package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RCContainerTest {

    @Test
    fun `parses header fields and config`() {
        val config = "{\"hello\":\"world\"}".toByteArray()
        val bytes = buildContainer(version = 1, flags = 0x07, config = config)

        val container = RCContainer.parse(bytes)

        assertThat(container.version).isEqualTo(1)
        assertThat(container.flags).isEqualTo(0x07)
        assertThat(container.config.readBytes()).isEqualTo(config)
        assertThat(container.elements).isEmpty()
    }

    @Test
    fun `parses a single element`() {
        val element = "payload-bytes".toByteArray()
        val bytes = buildContainer(config = "cfg".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.elements).hasSize(1)
        assertThat(container.elements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `parses multiple elements of differing sizes`() {
        val elements = listOf(
            "a".toByteArray(),
            "bb".toByteArray(),
            ByteArray(100) { it.toByte() },
            ByteArray(0),
        )
        val bytes = buildContainer(elements = elements)

        val container = RCContainer.parse(bytes)

        assertThat(container.elements).hasSize(elements.size)
        elements.forEachIndexed { index, expected ->
            assertThat(container.elements[index].data.readBytes()).isEqualTo(expected)
        }
    }

    @Test
    fun `skips config padding when config size is not a multiple of 8`() {
        // 3-byte config forces 5 bytes of padding before the first element.
        val element = "element".toByteArray()
        val bytes = buildContainer(config = "abc".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config.readBytes()).isEqualTo("abc".toByteArray())
        assertThat(container.elements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `parses empty config`() {
        val element = "x".toByteArray()
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config.remaining()).isEqualTo(0)
        assertThat(container.elements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `reads big-endian sizes`() {
        // 256-byte config encodes as 0x00,0x00,0x01,0x00 big-endian. A little-endian misread would
        // be 0x00010000 (65536) and overflow the buffer, so a successful parse proves big-endian.
        val config = ByteArray(256) { it.toByte() }
        val bytes = buildContainer(config = config)

        val container = RCContainer.parse(bytes)

        assertThat(container.config.remaining()).isEqualTo(256)
        assertThat(container.config.readBytes()).isEqualTo(config)
    }

    @Test
    fun `isChecksumValid returns true for a correct checksum`() {
        val element = "verify-me".toByteArray()
        val bytes = buildContainer(elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.elements[0].isChecksumValid()).isTrue()
    }

    @Test
    fun `isChecksumValid returns false for a corrupted element`() {
        val element = "verify-me".toByteArray()
        // Store a checksum that does not match the element bytes.
        val bytes = buildContainer(
            elements = listOf(element),
            checksumOverride = { _, _ -> ByteArray(32) { 0 } },
        )

        val container = RCContainer.parse(bytes)

        assertThat(container.elements[0].isChecksumValid()).isFalse()
    }

    @Test
    fun `element data is a zero-copy view over the source buffer`() {
        val element = "ABCD".toByteArray()
        // Empty config: element bytes begin at offset 8 (header) + 36 (checksum + size) = 44.
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))
        val elementOffset = 8 + 32 + 4

        val container = RCContainer.parse(bytes)
        val data = container.elements[0].data

        assertThat(data.get(0)).isEqualTo('A'.code.toByte())

        // Mutating the backing array is reflected in the view: no copy was made.
        bytes[elementOffset] = 'Z'.code.toByte()
        assertThat(data.get(0)).isEqualTo('Z'.code.toByte())
    }

    @Test
    fun `does not consume the caller's buffer position`() {
        val bytes = buildContainer(config = "cfg".toByteArray(), elements = listOf("e".toByteArray()))
        val buffer = ByteBuffer.wrap(bytes)

        RCContainer.parse(buffer)

        assertThat(buffer.position()).isEqualTo(0)
    }

    @Test
    fun `throws on buffer too small for header`() {
        assertThatThrownBy { RCContainer.parse(byteArrayOf(0x52, 0x43, 1, 0)) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws on invalid magic`() {
        val bytes = buildContainer().also {
            it[0] = 'X'.code.toByte()
        }
        assertThatThrownBy { RCContainer.parse(bytes) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws on unsupported version`() {
        val bytes = buildContainer(version = 2)
        assertThatThrownBy { RCContainer.parse(bytes) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws when config size exceeds buffer`() {
        val bytes = buildContainer(config = "abc".toByteArray())
        // Overwrite config_size (offset 4, big-endian) with a value far past the end.
        bytes[4] = 0x7F
        assertThatThrownBy { RCContainer.parse(bytes) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws on truncated element header`() {
        val bytes = buildContainer(config = ByteArray(0), elements = listOf("hi".toByteArray()))
        // Drop the trailing bytes so an element is declared but its header is incomplete.
        val truncated = bytes.copyOf(8 + 10)
        assertThatThrownBy { RCContainer.parse(truncated) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws when element size exceeds buffer`() {
        val bytes = buildContainer(config = ByteArray(0), elements = listOf("hi".toByteArray()))
        // element_size is the 4 bytes after the 32-byte checksum: offset 8 + 32 = 40.
        bytes[40] = 0x7F
        assertThatThrownBy { RCContainer.parse(bytes) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    private fun ByteBuffer.readBytes(): ByteArray {
        val copy = duplicate()
        val out = ByteArray(copy.remaining())
        copy.get(out)
        return out
    }

    @Suppress("MagicNumber")
    private fun buildContainer(
        version: Int = 1,
        flags: Int = 0,
        config: ByteArray = ByteArray(0),
        elements: List<ByteArray> = emptyList(),
        checksumOverride: ((index: Int, element: ByteArray) -> ByteArray)? = null,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write('R'.code)
        out.write('C'.code)
        out.write(version and 0xFF)
        out.write(flags and 0xFF)
        out.writeUInt32(config.size)
        out.write(config)
        out.padTo8()

        elements.forEachIndexed { index, element ->
            val checksum = checksumOverride?.invoke(index, element) ?: sha256(element)
            out.write(checksum)
            out.writeUInt32(element.size)
            out.write(element)
            out.padTo8()
        }
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.padTo8() {
        while (size() % 8 != 0) {
            write(0)
        }
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)
}
