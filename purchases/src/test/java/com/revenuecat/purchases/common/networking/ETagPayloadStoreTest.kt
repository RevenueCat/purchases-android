package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.CRC32

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ETagPayloadStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val url = "https://api.revenuecat.com/v1/subscribers/appUserID/offerings"

    private val directory: File by lazy { File(temporaryFolder.root, "payloads") }
    private val underTest: ETagPayloadStore by lazy { ETagPayloadStore(directory) }

    @Test
    fun `read returns null when nothing was written`() {
        assertThat(underTest.read(url, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `write and read round-trip a payload`() {
        val payload = "{\"offerings\":[{\"id\":\"premium\",\"desc\":\"a \\\"quoted\\\" name\"}]}\nwith\nnewlines"

        assertThat(writeAndReadBack(payload)).isEqualTo(payload)
    }

    @Test
    fun `write returns the payload checksum which read verifies`() {
        val payload = "ascii payload"

        val checksum = underTest.write(url, payload)

        assertThat(checksum).isEqualTo(crc32Of(payload))
        assertThat(underTest.read(url, expectedChecksum = checksum!!)).isEqualTo(payload)
    }

    @Test
    fun `a truncated payload file reads back as a miss`() {
        val checksum = underTest.write(url, "the full payload")!!
        overwritePayloadFile("the full pay")

        assertThat(underTest.read(url, expectedChecksum = checksum)).isNull()
    }

    @Test
    fun `a payload file of the same length but different content reads back as a miss`() {
        // Invisible to a size check, which is why the payload is checksummed instead.
        val checksum = underTest.write(url, "the full payload")!!
        overwritePayloadFile("the FULL payload")

        assertThat(underTest.read(url, expectedChecksum = checksum)).isNull()
    }

    @Test
    fun `round-trips multi-byte content larger than the encode buffer`() {
        // Surrogate pairs (4-byte UTF-8) sized well past the 64KB encode buffer, so pairs end up split
        // across buffer boundaries at many different offsets.
        val payload = buildString {
            append("{\"emoji\":\"")
            repeat(60_000) { index ->
                append("🎉")
                append('a' + (index % 3))
            }
            append("\"}")
        }

        assertThat(writeAndReadBack(payload)).isEqualTo(payload)
    }

    @Test
    fun `write overwrites the previous payload for the same url`() {
        underTest.write(url, "first")

        assertThat(writeAndReadBack("second")).isEqualTo("second")
    }

    @Test
    fun `payloads for different urls do not collide`() {
        val otherUrl = "$url#rc_payload"

        // Both written before either is read, or a store mapping every url to one file would pass.
        val checksum = underTest.write(url, "one")!!
        val otherChecksum = underTest.write(otherUrl, "two")!!

        assertThat(underTest.read(url, checksum)).isEqualTo("one")
        assertThat(underTest.read(otherUrl, otherChecksum)).isEqualTo("two")
    }

    @Test
    fun `clear removes all payloads`() {
        val checksum = underTest.write(url, "payload")!!
        underTest.clear()

        assertThat(underTest.read(url, expectedChecksum = checksum)).isNull()
        assertThat(directory.exists()).isFalse
    }

    @Test
    fun `a leftover temp file from a crashed write does not affect reads or later writes`() {
        val checksum = underTest.write(url, "good")!!
        val payloadFileName = directory.list()!!.single()
        File(directory, "$payloadFileName.tmp").writeText("partial write from a crashed process")

        assertThat(underTest.read(url, expectedChecksum = checksum)).isEqualTo("good")
        assertThat(writeAndReadBack("newer")).isEqualTo("newer")
    }

    @Test
    fun `write works again after clear`() {
        underTest.write(url, "payload")
        underTest.clear()

        assertThat(writeAndReadBack("again")).isEqualTo("again")
    }

    @Test
    fun `cleared payloads are trashed and deleted by the next write`() {
        underTest.write(url, "payload")

        underTest.clear()
        val afterClear = temporaryFolder.root.list()!!
        assertThat(afterClear).hasSize(1)
        assertThat(afterClear.single()).contains(".trash")

        underTest.write(url, "again")
        assertThat(temporaryFolder.root.list()!!).containsExactly("payloads")
    }

    @Test
    fun `write returns null when the directory cannot be created`() {
        val fileAsParent = temporaryFolder.newFile()
        val blocked = ETagPayloadStore(File(fileAsParent, "payloads"))

        assertThat(blocked.write(url, "payload")).isNull()
    }

    @Test
    fun `write returns null when the payload file cannot be replaced`() {
        val payloadFileName = underTest.write(url, "original").let { directory.list()!!.single() }
        val blockingDirectory = File(directory, payloadFileName)
        blockingDirectory.deleteRecursively()
        File(blockingDirectory, "child").apply { parentFile!!.mkdirs(); writeText("blocks the rename") }

        assertThat(underTest.write(url, "new payload")).isNull()
    }

    @Test
    fun `clear on a store that never wrote is a no-op`() {
        underTest.clear()

        assertThat(underTest.read(url, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `write returns null when the directory cannot be used`() {
        val blockedByFile = ETagPayloadStore(temporaryFolder.newFile())

        assertThat(blockedByFile.write(url, "payload")).isNull()
        assertThat(blockedByFile.read(url, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `an empty payload round-trips with a zero checksum`() {
        val checksum = underTest.write(url, "")

        assertThat(checksum).isEqualTo(0L)
        assertThat(underTest.read(url, expectedChecksum = 0L)).isEqualTo("")

        underTest.clear()
        // An empty payload checksums to 0, same as no bytes at all: the missing file must still miss.
        assertThat(underTest.read(url, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `a payload the encoder cannot represent fails the write instead of being altered`() {
        val payloadWithUnpairedSurrogate = "{\"key\":\"\uD83C\"}"

        assertThat(underTest.write(url, payloadWithUnpairedSurrogate)).isNull()
        assertThat(underTest.read(url, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `a payload file corrupted into invalid UTF-8 reads back as a miss instead of garbage`() {
        // Same length as the payload, so only the checksum catches it; lenient decoding would
        // otherwise hand back U+FFFD substitutions as a valid response.
        val checksum = underTest.write(url, "valid")!!
        File(directory, directory.list()!!.single()).writeBytes(byteArrayOf(0x7B, -1, -2, 0x22, 0x7D))

        assertThat(underTest.read(url, expectedChecksum = checksum)).isNull()
    }

    /** Reads back the way [ETagManager] does, with the checksum [ETagPayloadStore.write] returned. */
    private fun writeAndReadBack(payload: String, storeUrl: String = url): String? =
        underTest.write(storeUrl, payload)?.let { underTest.read(storeUrl, it) }

    private fun overwritePayloadFile(contents: String) {
        File(directory, directory.list()!!.single()).writeText(contents)
    }

    private fun crc32Of(payload: String): Long =
        CRC32().apply { update(payload.toByteArray(Charsets.UTF_8)) }.value
}
