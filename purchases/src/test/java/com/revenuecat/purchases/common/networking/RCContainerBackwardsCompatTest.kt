package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer

/**
 * Backwards-compatibility tests against committed, frozen RC Container Format v1 binaries.
 *
 * Unlike [RCContainerTest] (which builds bytes in-memory and so changes in lockstep with the
 * parser), these tests load `.bin` files captured once and committed to source control. Any
 * change that alters how the v1 wire format is parsed will break these tests, surfacing
 * accidental backwards-incompatible format regressions.
 *
 * To (re)generate the fixtures, run [generateFixtures] (see its KDoc).
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RCContainerBackwardsCompatTest {

    @Test
    fun `config-only fixture parses`() {
        val container = parseFixture("v1_config_only.bin")

        assertThat(container.version).isEqualTo(1)
        assertThat(container.flags).isEqualTo(0)
        assertThat(container.config.data.readBytes()).isEqualTo(RCContainerTestData.CONFIG_JSON)
        assertThat(container.config.isChecksumValid()).isTrue()
        assertThat(container.contentElements).isEmpty()
        assertThat(container.elements).isEmpty()
    }

    @Test
    fun `single-element fixture parses`() {
        val container = parseFixture("v1_single_element.bin")
        val blob = RCContainerTestData.WORKFLOW_BLOB

        assertThat(container.config.data.readBytes()).isEqualTo(RCContainerTestData.CONFIG_JSON)
        assertThat(container.contentElements).hasSize(1)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(blob)
        assertThat(container.contentElements[0].isChecksumValid()).isTrue()
        assertThat(container.elements[RCContainerTestData.refOf(blob)]!!.data.readBytes()).isEqualTo(blob)
    }

    @Test
    fun `multiple-elements fixture parses with differing sizes`() {
        val container = parseFixture("v1_multiple_elements.bin")
        val expected = listOf(
            RCContainerTestData.SMALL_BLOB,
            ByteArray(0),
            RCContainerTestData.WORKFLOW_BLOB,
            RCContainerTestData.LARGE_BLOB,
        )

        assertThat(container.config.data.readBytes()).isEqualTo(RCContainerTestData.CONFIG_JSON)
        assertThat(container.contentElements).hasSize(expected.size)
        expected.forEachIndexed { index, blob ->
            assertThat(container.contentElements[index].data.readBytes()).isEqualTo(blob)
            assertThat(container.contentElements[index].isChecksumValid()).isTrue()
        }
        // The 300-byte element proves little-endian size decoding survives round-trips.
        assertThat(container.contentElements[3].data.remaining()).isEqualTo(300)
    }

    @Test
    fun `empty-config fixture parses`() {
        val container = parseFixture("v1_empty_config.bin")
        val blob = RCContainerTestData.WORKFLOW_BLOB

        assertThat(container.config.data.remaining()).isEqualTo(0)
        assertThat(container.contentElements).hasSize(1)
        assertThat(container.contentElements[0].data.readBytes()).isEqualTo(blob)
    }

    @Test
    fun `flags-set fixture preserves header flags`() {
        val container = parseFixture("v1_flags_set.bin")

        assertThat(container.flags).isEqualTo(0x07)
        assertThat(container.config.data.readBytes()).isEqualTo(RCContainerTestData.CONFIG_JSON)
    }

    @Test
    fun `gzip-element fixture decodes to the uncompressed blob and verifies`() {
        val container = parseFixture("v1_gzip_element.bin")
        val blob = RCContainerTestData.WORKFLOW_BLOB

        assertThat(container.config.data.readBytes()).isEqualTo(RCContainerTestData.CONFIG_JSON)
        assertThat(container.contentElements).hasSize(1)
        val element = container.contentElements[0]
        assertThat(element.codec).isEqualTo(RCContentEncoding.GZIP.id)
        // The on-wire body is compressed, but decode() recovers the original and the checksum verifies.
        assertThat(element.data.readBytes()).isNotEqualTo(blob)
        assertThat(element.decode().readBytes()).isEqualTo(blob)
        assertThat(element.isChecksumValid()).isTrue()
        assertThat(container.elements[RCContainerTestData.refOf(blob)]!!.decode().readBytes()).isEqualTo(blob)
    }

    @Test
    fun `duplicate-elements fixture collapses in the content-addressed map`() {
        val container = parseFixture("v1_duplicate_elements.bin")

        // Both copies remain in the ordered list, but content-addressing collapses them in the map.
        assertThat(container.contentElements).hasSize(2)
        assertThat(container.elements).hasSize(1)
    }

    /**
     * Regenerates all committed fixtures under `src/test/resources/$FIXTURE_DIR`.
     *
     * Kept `@Ignore`d so it never runs in CI. To (re)generate after an intentional format change:
     * temporarily remove the `@Ignore`, run
     * `./gradlew :purchases:testDefaultsBc8DebugUnitTest --tests "*RCContainerBackwardsCompatTest*"`,
     * re-add `@Ignore`, then commit the resulting `.bin` files.
     */
    @Ignore("Run manually to regenerate the committed fixtures, then re-ignore.")
    @Test
    fun generateFixtures() {
        val dir = File("src/test/resources/${RCContainerTestData.FIXTURE_DIR}")
        dir.mkdirs()
        RCContainerTestData.ALL_FIXTURES.forEach { fixture ->
            File(dir, fixture.fileName).writeBytes(RCContainerTestData.buildContainer(fixture))
        }
    }

    private fun parseFixture(fileName: String): RCContainer {
        val bytes = javaClass.classLoader!!
            .getResource("${RCContainerTestData.FIXTURE_DIR}/$fileName")!!
            .openStream()
            .use { it.readBytes() }
        return RCContainer.parse(bytes)
    }

    private fun ByteBuffer.readBytes(): ByteArray {
        val copy = duplicate()
        val out = ByteArray(copy.remaining())
        copy.get(out)
        return out
    }
}
