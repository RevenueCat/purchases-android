package com.revenuecat.purchases.common.networking

import android.util.Base64
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
        assertThat(container.config.data.readBytes()).isEqualTo(config)
        assertThat(container.config.isChecksumValid()).isTrue()
        assertThat(container.contentElements).isEmpty()
        assertThat(container.elements).isEmpty()
    }

    @Test
    fun `parses a single content element`() {
        val element = "payload-bytes".toByteArray()
        val bytes = buildContainer(config = "cfg".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements).hasSize(1)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
        assertThat(container.elements[refOf(element)]!!.data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `parses multiple content elements of differing sizes`() {
        val elements = listOf(
            "a".toByteArray(),
            "bb".toByteArray(),
            ByteArray(100) { it.toByte() },
            ByteArray(0),
        )
        val bytes = buildContainer(elements = elements)

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements).hasSize(elements.size)
        elements.forEachIndexed { index, expected ->
            assertThat(container.contentElements[index].data.readBytes()).isEqualTo(expected)
            assertThat(container.elements[refOf(expected)]!!.data.readBytes()).isEqualTo(expected)
        }
    }

    @Test
    fun `looks up content elements by hex checksum`() {
        val element = "find-me".toByteArray()
        val bytes = buildContainer(elements = listOf(element))

        val container = RCContainer.parse(bytes)

        val ref = refOf(element)
        assertThat(ref).hasSize(32)
        assertThat(ref).doesNotContain("=")
        assertThat(container.elements).containsKey(ref)
        assertThat(container.elements[ref]!!.data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `identical content elements collapse to one map entry`() {
        val element = "duplicate".toByteArray()
        val bytes = buildContainer(elements = listOf(element, element.copyOf()))

        val container = RCContainer.parse(bytes)

        // Both are present in the ordered list, but content-addressing collapses them in the map.
        assertThat(container.contentElements).hasSize(2)
        assertThat(container.elements).hasSize(1)
    }

    @Test
    fun `skips config padding when config size is not a multiple of 8`() {
        // 3-byte config forces 5 bytes of content padding before the first content element.
        val element = "element".toByteArray()
        val bytes = buildContainer(config = "abc".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config.data.readBytes()).isEqualTo("abc".toByteArray())
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `parses empty config`() {
        val element = "x".toByteArray()
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config.data.remaining()).isEqualTo(0)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `reads little-endian sizes`() {
        // 256-byte element encodes as 0x00,0x01,0x00,0x00 little-endian. A big-endian misread would
        // be 0x00010000 (65536) and overflow the buffer, so a successful parse proves little-endian.
        val element = ByteArray(256) { it.toByte() }
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements[0].data.remaining()).isEqualTo(256)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `isChecksumValid returns true for a correct checksum`() {
        val element = "verify-me".toByteArray()
        val bytes = buildContainer(elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements[0].isChecksumValid()).isTrue()
    }

    @Test
    fun `isChecksumValid returns false for a corrupted element`() {
        val element = "verify-me".toByteArray()
        // Store a checksum that does not match the element bytes.
        val bytes = buildContainer(
            elements = listOf(element),
            checksumOverride = { _, _ -> ByteArray(24) { 0 } },
        )

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements[0].isChecksumValid()).isFalse()
    }

    @Test
    fun `config data is a zero-copy view over the source buffer`() {
        val config = "ABCD".toByteArray()
        // Element 0 (config) body begins at offset 8 (header) + 32 (checksum + size + reserved) = 40.
        val bytes = buildContainer(config = config)
        val configOffset = 8 + 24 + 4 + 4

        val container = RCContainer.parse(bytes)
        val data = container.config.data

        assertThat(data.get(0)).isEqualTo('A'.code.toByte())

        // Mutating the backing array is reflected in the view: no copy was made during parse.
        bytes[configOffset] = 'Z'.code.toByte()
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
    fun `parses from the buffer's current position`() {
        val container = buildContainer(config = "cfg".toByteArray(), elements = listOf("e".toByteArray()))
        val prefix = ByteArray(8) { 0xAB.toByte() } // unrelated, 8-aligned bytes before the container
        val combined = prefix + container

        val buffer = ByteBuffer.wrap(combined)
        buffer.position(prefix.size) // start parsing after the prefix

        val parsed = RCContainer.parse(buffer)

        assertThat(parsed.config.data.readBytes()).isEqualTo("cfg".toByteArray())
        assertThat(parsed.contentElements[0].data.readBytes()).isEqualTo("e".toByteArray())
        // The overload must not consume the caller's position.
        assertThat(buffer.position()).isEqualTo(prefix.size)
    }

    @Test
    fun `parses from a non-8-aligned position`() {
        val container = buildContainer(config = "abc".toByteArray(), elements = listOf("element".toByteArray()))
        val prefix = ByteArray(3) { 0xAB.toByte() } // NOT a multiple of 8
        val combined = prefix + container

        val buffer = ByteBuffer.wrap(combined)
        buffer.position(prefix.size)

        val parsed = RCContainer.parse(buffer)

        assertThat(parsed.config.data.readBytes()).isEqualTo("abc".toByteArray())
        assertThat(parsed.contentElements[0].data.readBytes()).isEqualTo("element".toByteArray())
        assertThat(buffer.position()).isEqualTo(prefix.size)
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
    fun `throws when there is no config element`() {
        // Header only, no elements.
        val header = buildContainer(config = ByteArray(0)).copyOf(8)
        assertThatThrownBy { RCContainer.parse(header) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws on truncated element header`() {
        val bytes = buildContainer(config = ByteArray(0), elements = listOf("hi".toByteArray()))
        // Drop trailing bytes so the second (content) element header is incomplete.
        // Config occupies header(8) + element header(32) + body(0) = 40; cut partway into the next header.
        val truncated = bytes.copyOf(40 + 10)
        assertThatThrownBy { RCContainer.parse(truncated) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `throws when element size exceeds buffer`() {
        val bytes = buildContainer(config = ByteArray(0), elements = listOf("hi".toByteArray()))
        // Config's element_size is the 4 bytes after the 24-byte checksum: offset 8 + 24 = 32.
        // Set its most-significant little-endian byte high to declare a huge size past EOF.
        bytes[35] = 0x7F
        assertThatThrownBy { RCContainer.parse(bytes) }
            .isInstanceOf(RCContainerFormatException::class.java)
    }

    private fun ByteBuffer.readBytes(): ByteArray {
        val copy = duplicate()
        val out = ByteArray(copy.remaining())
        copy.get(out)
        return out
    }

    /** Content-addressed ref: SHA-256 truncated to 24 bytes, URL-safe base64 (no padding). */
    private fun refOf(element: ByteArray): String =
        Base64.encodeToString(sha256(element), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

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
        repeat(4) { out.write(0) } // header reserved

        // Element 0 is always the config, followed by the content elements.
        val allElements = listOf(config) + elements
        allElements.forEachIndexed { index, element ->
            val checksum = checksumOverride?.invoke(index, element) ?: sha256(element)
            out.write(checksum)
            out.writeUInt32(element.size)
            out.writeUInt32(0) // element reserved
            out.write(element)
            out.padTo8()
        }
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    private fun ByteArrayOutputStream.padTo8() {
        while (size() % 8 != 0) {
            write(0)
        }
    }

    /** SHA-256 truncated to the format's 24-byte (192-bit) checksum. */
    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data).copyOf(24)
}
