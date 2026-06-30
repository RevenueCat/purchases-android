package com.revenuecat.purchases.common.networking

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

/**
 * Shared builders and fixture definitions for RC Container Format tests.
 *
 * Both the in-memory parser tests ([RCContainerTest]) and the on-disk backwards-compatibility
 * tests ([RCContainerBackwardsCompatTest]) use these so the byte layout used to generate fixtures
 * and the expectations asserted against them never drift apart.
 */
@Suppress("MagicNumber")
internal object RCContainerTestData {

    /** Directory (under test resources) holding the committed, frozen `.bin` fixtures. */
    const val FIXTURE_DIR = "rc_container"

    /**
     * Arbitrary placeholder workflow blobs, the kind of static content the config below references by ref.
     * The exact shape is not asserted anywhere; they only need stable bytes so their content-address is stable.
     * Declared before [CONFIG_JSON_TEXT] because that payload embeds their content-addresses (via [refOf]).
     */
    // language=json
    private val WORKFLOW_BLOB_TEXT: String = """
        {
          "id": "wf1234",
          "steps": [ { "type": "paywall", "offering": "default" } ]
        }
    """.trimIndent()

    val WORKFLOW_BLOB: ByteArray = WORKFLOW_BLOB_TEXT.toByteArray()

    // language=json
    private val SUMMER_WORKFLOW_BLOB_TEXT: String = """
        {
          "id": "wf5678",
          "steps": [ { "type": "paywall", "offering": "summerCampaign" } ]
        }
    """.trimIndent()

    val SUMMER_WORKFLOW_BLOB: ByteArray = SUMMER_WORKFLOW_BLOB_TEXT.toByteArray()

    /**
     * A representative `/v1/config` payload, modeled on `get_remote_config_success.json`. Stored as the config
     * element (element 0) of most fixtures so they resemble real wire data.
     *
     * Uses the `workflows` topic: each item is keyed by the workflow `rc_public_id` and carries an inline
     * `offering_identifier` (the human-readable offering ID) plus a `blob_ref` pointing at the workflow's static
     * blob. The refs below are the real content-addresses of [WORKFLOW_BLOB] / [SUMMER_WORKFLOW_BLOB], so a
     * container that inlines those blobs round-trips through the manager end-to-end.
     */
    // language=json
    private val CONFIG_JSON_TEXT: String = """
        {
          "domain": "app",
          "manifest": "v1.1710000000.workflows:etag1",
          "active_topics": ["workflows"],
          "prefetch_blobs": ["${refOf(WORKFLOW_BLOB)}"],
          "topics": {
            "workflows": {
              "wf1234": { "offering_identifier": "default", "blob_ref": "${refOf(WORKFLOW_BLOB)}" },
              "wf5678": { "offering_identifier": "summerCampaign", "blob_ref": "${refOf(SUMMER_WORKFLOW_BLOB)}" }
            }
          }
        }
    """.trimIndent()

    val CONFIG_JSON: ByteArray = CONFIG_JSON_TEXT.toByteArray()

    /** A >255-byte body, so its size encodes across multiple little-endian bytes (0x2C 0x01 = 300). */
    val LARGE_BLOB: ByteArray = ByteArray(300) { (it % 256).toByte() }

    /** A small body to mix sizes within a single container. */
    val SMALL_BLOB: ByteArray = "a".toByteArray()

    /**
     * The frozen set of fixtures. Order is irrelevant. The bytes produced from these specs by
     * [buildContainer] are committed once under `src/test/resources/$FIXTURE_DIR` and must keep
     * parsing identically forever.
     */
    val ALL_FIXTURES: List<RCContainerFixture> = listOf(
        RCContainerFixture(
            fileName = "v1_config_only.bin",
            config = CONFIG_JSON,
        ),
        RCContainerFixture(
            fileName = "v1_single_element.bin",
            config = CONFIG_JSON,
            elements = listOf(WORKFLOW_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_multiple_elements.bin",
            config = CONFIG_JSON,
            elements = listOf(SMALL_BLOB, ByteArray(0), WORKFLOW_BLOB, LARGE_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_empty_config.bin",
            config = ByteArray(0),
            elements = listOf(WORKFLOW_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_flags_set.bin",
            flags = 0x07,
            config = CONFIG_JSON,
        ),
        RCContainerFixture(
            fileName = "v1_duplicate_elements.bin",
            config = CONFIG_JSON,
            elements = listOf(WORKFLOW_BLOB, WORKFLOW_BLOB.copyOf()),
        ),
        RCContainerFixture(
            fileName = "v1_gzip_element.bin",
            config = CONFIG_JSON,
            elements = listOf(WORKFLOW_BLOB),
            // Element index 1 (the content blob) is gzipped on the wire; config (index 0) stays uncompressed.
            codecForIndex = { index -> if (index == 1) RCContentEncoding.GZIP.id else RCContentEncoding.NONE.id },
        ),
    )

    /**
     * Serializes a fixture/spec to RC Container Format v1 bytes.
     *
     * [elements]/[config] are the **uncompressed** payloads. [codecForIndex] picks a codec id per element
     * (index 0 is the config); when it returns [RCContentEncoding.GZIP] the body is gzipped on the wire while
     * the checksum still covers the uncompressed bytes (so the content-address is unchanged). Any other codec
     * id is written verbatim into the reserved low byte with the raw (un-encoded) body, for unsupported-codec
     * tests.
     */
    fun buildContainer(
        version: Int = 1,
        flags: Int = 0,
        config: ByteArray = ByteArray(0),
        elements: List<ByteArray> = emptyList(),
        checksumOverride: ((index: Int, element: ByteArray) -> ByteArray)? = null,
        codecForIndex: (index: Int) -> Int = { RCContentEncoding.NONE.id },
        reservedForIndex: ((index: Int) -> Long)? = null,
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
            val codec = codecForIndex(index)
            val checksum = checksumOverride?.invoke(index, element) ?: sha256(element)
            val onWire = if (codec == RCContentEncoding.GZIP.id) gzip(element) else element
            // The reserved u32's low byte is the codec id; reservedForIndex lets tests set the upper bits.
            val reserved = reservedForIndex?.invoke(index) ?: (codec.toLong() and 0xFF)
            out.write(checksum)
            out.writeUInt32(onWire.size)
            out.writeUInt32(reserved.toInt())
            out.write(onWire)
            out.padTo8()
        }
        return out.toByteArray()
    }

    /** Gzip-compresses [data] (matches the wire format the backend produces for the GZIP codec). */
    fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    fun buildContainer(fixture: RCContainerFixture): ByteArray = buildContainer(
        version = fixture.version,
        flags = fixture.flags,
        config = fixture.config,
        elements = fixture.elements,
        codecForIndex = fixture.codecForIndex,
    )

    /** Content-addressed ref: SHA-256 truncated to 24 bytes, URL-safe base64 (no padding). */
    fun refOf(element: ByteArray): String =
        Base64.encodeToString(sha256(element), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /** SHA-256 truncated to the format's 24-byte (192-bit) checksum. */
    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data).copyOf(24)

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
}

/** Declarative spec for a committed container fixture. */
internal class RCContainerFixture(
    val fileName: String,
    val version: Int = 1,
    val flags: Int = 0,
    val config: ByteArray,
    val elements: List<ByteArray> = emptyList(),
    val codecForIndex: (index: Int) -> Int = { RCContentEncoding.NONE.id },
)
