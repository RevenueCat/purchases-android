package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.LogMessage
import com.revenuecat.purchases.assertWarnLog
import com.revenuecat.purchases.common.Config as PurchasesConfig
import com.revenuecat.purchases.common.currentLogHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

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
        assertThat(container.config).isEqualTo(config)
        assertThat(container.contentElements).isEmpty()
    }

    @Test
    fun `warns and still parses when header flags are non-zero`() {
        val bytes = buildContainer(version = 1, flags = 0x07, config = "cfg".toByteArray())

        assertWarnLog("RC Container header flags non-zero (0x7); ignoring unknown flags.") {
            val container = RCContainer.parse(bytes)
            // Parsing remains forward-compatible: the flags are preserved, not rejected.
            assertThat(container.flags).isEqualTo(0x07)
        }
    }

    @Test
    fun `does not warn when header flags are zero`() {
        val bytes = buildContainer(version = 1, flags = 0, config = "cfg".toByteArray())

        val logs = captureLogs { RCContainer.parse(bytes) }

        assertThat(logs.filter { it.level == LogLevel.WARN }).isEmpty()
    }

    @Test
    fun `warns and still parses when element reserved upper bits are non-zero`() {
        // reserved = 0x00000100: codec low byte is 0 (NONE), the upper bits carry an unknown flag.
        val bytes = buildContainer(
            config = "cfg".toByteArray(),
            elements = listOf("payload".toByteArray()),
            reservedForIndex = { index -> if (index == 1) 0x100L else 0L },
        )

        assertWarnLog("RC element reserved bits non-zero (0x100); ignoring unknown reserved bits.") {
            val container = RCContainer.parse(bytes)
            assertThat(container.contentElements[0].data.readBytes()).isEqualTo("payload".toByteArray())
        }
    }

    @Test
    fun `does not warn when only the element codec byte is set`() {
        // A legitimate non-zero codec (GZIP) lives in the reserved low byte; the upper bits stay zero.
        val bytes = buildContainer(
            config = "cfg".toByteArray(),
            elements = listOf("compress-me-".repeat(20).toByteArray()),
            codecForIndex = { index -> if (index == 1) RCContentEncoding.GZIP.id else RCContentEncoding.NONE.id },
        )

        val logs = captureLogs { RCContainer.parse(bytes) }

        assertThat(logs.filter { it.level == LogLevel.WARN }).isEmpty()
    }

    @Test
    fun `parses a single content element`() {
        val element = "payload-bytes".toByteArray()
        val bytes = buildContainer(config = "cfg".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.contentElements).hasSize(1)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
        // The element is content-addressed by the hash of its (uncompressed) bytes.
        assertThat(container.contentElements[0].checksumBase64()).isEqualTo(refOf(element))
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
            assertThat(container.contentElements[index].checksumBase64()).isEqualTo(refOf(expected))
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
        assertThat(container.contentElements.single().checksumBase64()).isEqualTo(ref)
    }

    @Test
    fun `identical content elements are parsed as separate elements sharing a ref`() {
        val element = "duplicate".toByteArray()
        val bytes = buildContainer(elements = listOf(element, element.copyOf()))

        val container = RCContainer.parse(bytes)

        // Both copies are present in the ordered list, addressed by the same content ref.
        assertThat(container.contentElements).hasSize(2)
        assertThat(container.contentElements.map { it.checksumBase64() })
            .containsExactly(refOf(element), refOf(element))
    }

    @Test
    fun `skips config padding when config size is not a multiple of 8`() {
        // 3-byte config forces 5 bytes of content padding before the first content element.
        val element = "element".toByteArray()
        val bytes = buildContainer(config = "abc".toByteArray(), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config).isEqualTo("abc".toByteArray())
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(element)
    }

    @Test
    fun `parses empty config`() {
        val element = "x".toByteArray()
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))

        val container = RCContainer.parse(bytes)

        assertThat(container.config).isEmpty()
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
    fun `gzip element decodes to its uncompressed bytes and verifies`() {
        val element = "compress-me-".repeat(20).toByteArray()
        val bytes = buildContainer(
            elements = listOf(element),
            codecForIndex = { index -> if (index == 1) RCContentEncoding.GZIP.id else RCContentEncoding.NONE.id },
        )

        val container = RCContainer.parse(bytes)
        val parsed = container.contentElements[0]

        assertThat(parsed.codec).isEqualTo(RCContentEncoding.GZIP.id)
        // The on-wire bytes are compressed (and smaller than the original)...
        assertThat(parsed.data.readBytes()).isNotEqualTo(element)
        assertThat(parsed.data.remaining()).isLessThan(element.size)
        // ...but decode() yields the original (verifying against the uncompressed bytes as it does).
        assertThat(parsed.decode()).isEqualTo(element)
        // The content address is the hash of the uncompressed bytes, unchanged by compression.
        assertThat(parsed.checksumBase64()).isEqualTo(refOf(element))
    }

    @Test
    fun `gzip config element is decoded during parsing`() {
        val config = "{\"hello\":\"world\"}".toByteArray()
        // The config is gzipped on the wire; parse must decode it into container.config.
        val bytes = buildContainer(config = config, codecForIndex = { RCContentEncoding.GZIP.id })

        val container = RCContainer.parse(bytes)

        assertThat(container.config).isEqualTo(config)
    }

    @Test
    fun `parse throws when the config element checksum does not match its bytes`() {
        // Store a checksum for the config (element 0) that does not match its bytes.
        val bytes = buildContainer(
            config = "{\"hello\":\"world\"}".toByteArray(),
            checksumOverride = { _, _ -> ByteArray(24) { 0 } },
        )

        assertThatThrownBy { RCContainer.parse(bytes) }.isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `contentElements decode to their uncompressed bytes and re-iterate`() {
        val blobs = listOf("a".toByteArray(), "bb".toByteArray(), "ccc".repeat(30).toByteArray())
        val container = RCContainer.parse(buildContainer(elements = blobs))

        assertThat(container.contentElements.map { it.decode() }).containsExactlyElementsOf(blobs)
        // Iterating doesn't consume: the elements are still there for another pass.
        assertThat(container.contentElements.map { it.decode() }).containsExactlyElementsOf(blobs)
    }

    @Test
    fun `decode throws when the element checksum does not match its bytes`() {
        val blob = "verify-me".toByteArray()
        // Store a checksum that does not match the (content) element bytes.
        val bytes = buildContainer(
            elements = listOf(blob),
            checksumOverride = { index, element -> if (index == 1) ByteArray(24) { 0 } else RCContainerTestData.sha256(element) },
        )

        val parsed = RCContainer.parse(bytes).contentElements.single()

        // The bytes decompress, but they do not hash to the (tampered) checksum, so decode() rejects them.
        assertThatThrownBy { parsed.decode() }.isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `unsupported codec makes decode throw`() {
        listOf(RCContentEncoding.BROTLI.id, RCContentEncoding.ZSTD.id, 9).forEach { codecId ->
            val element = "payload".toByteArray()
            val bytes = buildContainer(
                elements = listOf(element),
                codecForIndex = { index -> if (index == 1) codecId else RCContentEncoding.NONE.id },
            )

            val parsed = RCContainer.parse(bytes).contentElements[0]

            assertThat(parsed.codec).isEqualTo(codecId)
            assertThatThrownBy { parsed.decode() }.isInstanceOf(RCContainerFormatException::class.java)
        }
    }

    @Test
    fun `corrupt gzip body makes decode throw`() {
        val element = "compress-me-".repeat(20).toByteArray()
        val bytes = buildContainer(
            elements = listOf(element),
            codecForIndex = { index -> if (index == 1) RCContentEncoding.GZIP.id else RCContentEncoding.NONE.id },
        )
        // Layout: header(8) + empty-config element header(32) + content element header(32) = 72.
        // Offset 72 is the gzip body's first magic byte; corrupting it breaks decompression.
        val gzipBodyStart = 8 + 32 + 32
        bytes[gzipBodyStart] = (bytes[gzipBodyStart] + 1).toByte()

        val parsed = RCContainer.parse(bytes).contentElements[0]

        assertThatThrownBy { parsed.decode() }.isInstanceOf(RCContainerFormatException::class.java)
    }

    @Test
    fun `content element data is a zero-copy view over the source buffer`() {
        val element = "ABCD".toByteArray()
        // Empty config: header(8) + config elem header(32) + config body(0) + content elem header(32) = 72.
        val bytes = buildContainer(config = ByteArray(0), elements = listOf(element))
        val elementOffset = 8 + 32 + 32

        val container = RCContainer.parse(bytes)
        val data = container.contentElements[0].data

        assertThat(data.get(0)).isEqualTo('A'.code.toByte())

        // Mutating the backing array is reflected in the view: no copy was made during parse.
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
    fun `parses from the buffer's current position`() {
        val container = buildContainer(config = "cfg".toByteArray(), elements = listOf("e".toByteArray()))
        val prefix = ByteArray(8) { 0xAB.toByte() } // unrelated, 8-aligned bytes before the container
        val combined = prefix + container

        val buffer = ByteBuffer.wrap(combined)
        buffer.position(prefix.size) // start parsing after the prefix

        val parsed = RCContainer.parse(buffer)

        assertThat(parsed.config).isEqualTo("cfg".toByteArray())
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

        assertThat(parsed.config).isEqualTo("abc".toByteArray())
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

    private fun refOf(element: ByteArray): String = RCContainerTestData.refOf(element)

    private fun buildContainer(
        version: Int = 1,
        flags: Int = 0,
        config: ByteArray = ByteArray(0),
        elements: List<ByteArray> = emptyList(),
        checksumOverride: ((index: Int, element: ByteArray) -> ByteArray)? = null,
        codecForIndex: (index: Int) -> Int = { RCContentEncoding.NONE.id },
        reservedForIndex: ((index: Int) -> Long)? = null,
    ): ByteArray =
        RCContainerTestData.buildContainer(
            version,
            flags,
            config,
            elements,
            checksumOverride,
            codecForIndex,
            reservedForIndex,
        )

    /** Captures every log emitted while [block] runs (with logging forced to VERBOSE). */
    private fun captureLogs(block: () -> Unit): List<LogMessage> {
        val logs = mutableListOf<LogMessage>()
        val previousLogLevel = PurchasesConfig.logLevel
        val previousLogHandler = currentLogHandler
        PurchasesConfig.logLevel = LogLevel.VERBOSE
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) { logs.add(LogMessage(LogLevel.VERBOSE, msg)) }
            override fun d(tag: String, msg: String) { logs.add(LogMessage(LogLevel.DEBUG, msg)) }
            override fun i(tag: String, msg: String) { logs.add(LogMessage(LogLevel.INFO, msg)) }
            override fun w(tag: String, msg: String) { logs.add(LogMessage(LogLevel.WARN, msg)) }
            override fun e(tag: String, msg: String, throwable: Throwable?) {
                logs.add(LogMessage(LogLevel.ERROR, msg, throwable))
            }
        }
        try {
            block()
        } finally {
            currentLogHandler = previousLogHandler
            PurchasesConfig.logLevel = previousLogLevel
        }
        return logs
    }
}
