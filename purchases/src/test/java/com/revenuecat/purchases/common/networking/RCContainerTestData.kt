package com.revenuecat.purchases.common.networking

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

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
     * A representative `/v1/config` payload, modeled on `get_remote_config_success.json`. Stored as
     * the config element (element 0) of most fixtures so they resemble real wire data.
     */
    // language=json
    private val CONFIG_JSON_TEXT: String = """
        {
          "api_sources": [
            {
              "id": "primary",
              "url": "https://api.revenuecat.com/",
              "priority": 0,
              "weight": 100
            }
          ],
          "blob_sources": [
            {
              "id": "cloudfront-primary",
              "url_format": "https://assets.revenuecat.com/rc_app_1234/{blob_ref}",
              "priority": 0,
              "weight": 100
            }
          ],
          "manifest": {
            "topics": {
              "product_entitlement_mapping": {
                "DEFAULT": {
                  "blob_ref": "6a4d0f53d9f6b8e2f4dca0fd1c7c4f5e3e1b1ef0f45d989e2f8f8d0d91ec1b6a"
                }
              }
            }
          }
        }
    """.trimIndent()

    val CONFIG_JSON: ByteArray = CONFIG_JSON_TEXT.toByteArray()

    /** A JSON-ish content blob, the kind of thing the config above would reference by ref. */
    // language=json
    private val ENTITLEMENT_MAPPING_TEXT: String = """
        {
          "products": {
            "monthly": ["pro"],
            "annual": ["pro", "plus"]
          }
        }
    """.trimIndent()

    val ENTITLEMENT_MAPPING_BLOB: ByteArray = ENTITLEMENT_MAPPING_TEXT.toByteArray()

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
            elements = listOf(ENTITLEMENT_MAPPING_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_multiple_elements.bin",
            config = CONFIG_JSON,
            elements = listOf(SMALL_BLOB, ByteArray(0), ENTITLEMENT_MAPPING_BLOB, LARGE_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_empty_config.bin",
            config = ByteArray(0),
            elements = listOf(ENTITLEMENT_MAPPING_BLOB),
        ),
        RCContainerFixture(
            fileName = "v1_flags_set.bin",
            flags = 0x07,
            config = CONFIG_JSON,
        ),
        RCContainerFixture(
            fileName = "v1_duplicate_elements.bin",
            config = CONFIG_JSON,
            elements = listOf(ENTITLEMENT_MAPPING_BLOB, ENTITLEMENT_MAPPING_BLOB.copyOf()),
        ),
    )

    /** Serializes a fixture/spec to RC Container Format v1 bytes. */
    fun buildContainer(
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

    fun buildContainer(fixture: RCContainerFixture): ByteArray = buildContainer(
        version = fixture.version,
        flags = fixture.flags,
        config = fixture.config,
        elements = fixture.elements,
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
)
