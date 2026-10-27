package com.revenuecat.purchases.common.caching

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
class PayloadFileStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val key = "https://api.revenuecat.com/v1/subscribers/appUserID/offerings"

    private val directory: File by lazy { File(temporaryFolder.root, "payloads") }
    private val underTest: PayloadFileStore by lazy { PayloadFileStore(directory) }

    @Test
    fun `base directory constructor stores files under the RevenueCat vendor directory`() {
        val store = PayloadFileStore(temporaryFolder.root, "named_store")

        store.write(key, "payload")

        assertThat(File(temporaryFolder.root, "RevenueCat/named_store").list()).hasSize(1)
    }

    @Test
    fun `a null base directory does not fail construction`() {
        PayloadFileStore(null, "named_store")
    }

    @Test
    fun `a synced write round-trips`() {
        val store = PayloadFileStore(directory, syncWrites = true)

        val checksum = store.write(key, "payload")!!

        assertThat(store.read(key, checksum)).isEqualTo("payload")
    }

    @Test
    fun `read returns null when nothing was written`() {
        assertThat(underTest.read(key, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `write and read round-trip a payload`() {
        val payload = "{\"offerings\":[{\"id\":\"premium\",\"desc\":\"a \\\"quoted\\\" name\"}]}\nwith\nnewlines"

        assertThat(writeAndReadBack(payload)).isEqualTo(payload)
    }

    @Test
    fun `write returns the payload checksum which read verifies`() {
        val payload = "ascii payload"

        val checksum = underTest.write(key, payload)

        assertThat(checksum).isEqualTo(crc32Of(payload))
        assertThat(underTest.read(key, expectedChecksum = checksum!!)).isEqualTo(payload)
    }

    @Test
    fun `a truncated payload file reads back as a miss`() {
        val checksum = underTest.write(key, "the full payload")!!
        overwritePayloadFile("the full pay")

        assertThat(underTest.read(key, expectedChecksum = checksum)).isNull()
    }

    @Test
    fun `a payload file of the same length but different content reads back as a miss`() {
        // Invisible to a size check, which is why the payload is checksummed instead.
        val checksum = underTest.write(key, "the full payload")!!
        overwritePayloadFile("the FULL payload")

        assertThat(underTest.read(key, expectedChecksum = checksum)).isNull()
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
    fun `write overwrites the previous payload for the same key`() {
        underTest.write(key, "first")

        assertThat(writeAndReadBack("second")).isEqualTo("second")
    }

    @Test
    fun `payloads for different keys do not collide`() {
        val otherKey = "$key#rc_payload"

        // Both written before either is read, or a store mapping every key to one file would pass.
        val checksum = underTest.write(key, "one")!!
        val otherChecksum = underTest.write(otherKey, "two")!!

        assertThat(underTest.read(key, checksum)).isEqualTo("one")
        assertThat(underTest.read(otherKey, otherChecksum)).isEqualTo("two")
    }

    @Test
    fun `clear removes all payloads`() {
        val checksum = underTest.write(key, "payload")!!
        underTest.clear()

        assertThat(underTest.read(key, expectedChecksum = checksum)).isNull()
        assertThat(directory.exists()).isFalse
    }

    @Test
    fun `delete removes only that key's payload`() {
        val checksum = underTest.write(key, "payload")!!
        val otherChecksum = underTest.write("other_key", "other")!!

        underTest.delete(key)

        assertThat(underTest.read(key, expectedChecksum = checksum)).isNull()
        assertThat(underTest.read("other_key", expectedChecksum = otherChecksum)).isEqualTo("other")
    }

    @Test
    fun `a leftover temp file from a crashed write does not affect reads or later writes`() {
        val checksum = underTest.write(key, "good")!!
        val payloadFileName = directory.list()!!.single()
        File(directory, "$payloadFileName.tmp").writeText("partial write from a crashed process")

        assertThat(underTest.read(key, expectedChecksum = checksum)).isEqualTo("good")
        assertThat(writeAndReadBack("newer")).isEqualTo("newer")
    }

    @Test
    fun `write works again after clear`() {
        underTest.write(key, "payload")
        underTest.clear()

        assertThat(writeAndReadBack("again")).isEqualTo("again")
    }

    @Test
    fun `cleared payloads are trashed and deleted by the next write`() {
        underTest.write(key, "payload")

        underTest.clear()
        val afterClear = temporaryFolder.root.list()!!
        assertThat(afterClear).hasSize(1)
        assertThat(afterClear.single()).contains(".trash")

        underTest.write(key, "again")
        assertThat(temporaryFolder.root.list()!!).containsExactly("payloads")
    }

    @Test
    fun `write returns null when the directory cannot be created`() {
        val fileAsParent = temporaryFolder.newFile()
        val blocked = PayloadFileStore(File(fileAsParent, "payloads"))

        assertThat(blocked.write(key, "payload")).isNull()
    }

    @Test
    fun `write returns null when the payload file cannot be replaced`() {
        val payloadFileName = underTest.write(key, "original").let { directory.list()!!.single() }
        val blockingDirectory = File(directory, payloadFileName)
        blockingDirectory.deleteRecursively()
        File(blockingDirectory, "child").apply { parentFile!!.mkdirs(); writeText("blocks the rename") }

        assertThat(underTest.write(key, "new payload")).isNull()
    }

    @Test
    fun `clear on a store that never wrote is a no-op`() {
        underTest.clear()

        assertThat(underTest.read(key, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `write returns null when the directory cannot be used`() {
        val blockedByFile = PayloadFileStore(temporaryFolder.newFile())

        assertThat(blockedByFile.write(key, "payload")).isNull()
        assertThat(blockedByFile.read(key, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `an empty payload round-trips with a zero checksum`() {
        val checksum = underTest.write(key, "")

        assertThat(checksum).isEqualTo(0L)
        assertThat(underTest.read(key, expectedChecksum = 0L)).isEqualTo("")

        underTest.clear()
        // An empty payload checksums to 0, same as no bytes at all: the missing file must still miss.
        assertThat(underTest.read(key, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `a payload the encoder cannot represent fails the write instead of being altered`() {
        val payloadWithUnpairedSurrogate = "{\"key\":\"\uD83C\"}"

        assertThat(underTest.write(key, payloadWithUnpairedSurrogate)).isNull()
        assertThat(underTest.read(key, expectedChecksum = 0L)).isNull()
    }

    @Test
    fun `a payload file corrupted into invalid UTF-8 reads back as a miss instead of garbage`() {
        // Same length as the payload, so only the checksum catches it; lenient decoding would
        // otherwise hand back U+FFFD substitutions as a valid response.
        val checksum = underTest.write(key, "valid")!!
        File(directory, directory.list()!!.single()).writeBytes(byteArrayOf(0x7B, -1, -2, 0x22, 0x7D))

        assertThat(underTest.read(key, expectedChecksum = checksum)).isNull()
    }

    @Test
    fun `checksumOf matches the checksum write returns without writing a file`() {
        val payload = "{\"emoji\":\"🎉\"}"

        val checksum = underTest.checksumOf(payload)

        assertThat(directory.exists()).isFalse
        assertThat(checksum).isEqualTo(underTest.write(key, payload))
    }

    @Test
    fun `checksumOf returns null for a payload the encoder cannot represent`() {
        assertThat(underTest.checksumOf("{\"key\":\"\uD83C\"}")).isNull()
    }

    @Test
    fun `contains reports whether a payload was written for the key`() {
        assertThat(underTest.contains(key)).isFalse

        underTest.write(key, "payload")
        assertThat(underTest.contains(key)).isTrue
        assertThat(underTest.contains("other")).isFalse

        underTest.clear()
        assertThat(underTest.contains(key)).isFalse
    }

    private fun writeAndReadBack(payload: String, storeKey: String = key): String? =
        underTest.write(storeKey, payload)?.let { underTest.read(storeKey, it) }

    private fun overwritePayloadFile(contents: String) {
        File(directory, directory.list()!!.single()).writeText(contents)
    }

    private fun crc32Of(payload: String): Long =
        CRC32().apply { update(payload.toByteArray(Charsets.UTF_8)) }.value
}
