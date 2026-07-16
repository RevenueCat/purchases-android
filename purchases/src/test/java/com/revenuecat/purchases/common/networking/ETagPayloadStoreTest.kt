package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

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
        assertThat(underTest.read(url)).isNull()
    }

    @Test
    fun `write and read round-trip a payload`() {
        val payload = "{\"offerings\":[{\"id\":\"premium\",\"desc\":\"a \\\"quoted\\\" name\"}]}\nwith\nnewlines"

        assertThat(underTest.write(url, payload)).isNotNull
        assertThat(underTest.read(url)).isEqualTo(payload)
    }

    @Test
    fun `write returns the encoded size which read verifies`() {
        val payload = "ascii payload"

        val sizeBytes = underTest.write(url, payload)

        assertThat(sizeBytes).isEqualTo(payload.length.toLong())
        assertThat(underTest.read(url, expectedSizeBytes = sizeBytes)).isEqualTo(payload)
    }

    @Test
    fun `a payload file with an unexpected size reads back as a miss`() {
        val sizeBytes = underTest.write(url, "the full payload")!!
        val payloadFile = File(directory, directory.list()!!.single())
        payloadFile.writeText("the full pay")

        assertThat(underTest.read(url, expectedSizeBytes = sizeBytes)).isNull()
        assertThat(underTest.read(url)).isEqualTo("the full pay")
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

        assertThat(underTest.write(url, payload)).isNotNull
        assertThat(underTest.read(url)).isEqualTo(payload)
    }

    @Test
    fun `write overwrites the previous payload for the same url`() {
        underTest.write(url, "first")
        underTest.write(url, "second")

        assertThat(underTest.read(url)).isEqualTo("second")
    }

    @Test
    fun `payloads for different urls do not collide`() {
        val otherUrl = "$url#rc_payload"
        underTest.write(url, "one")
        underTest.write(otherUrl, "two")

        assertThat(underTest.read(url)).isEqualTo("one")
        assertThat(underTest.read(otherUrl)).isEqualTo("two")
    }

    @Test
    fun `clear removes all payloads`() {
        underTest.write(url, "payload")
        underTest.clear()

        assertThat(underTest.read(url)).isNull()
        assertThat(directory.exists()).isFalse
    }

    @Test
    fun `a leftover temp file from a crashed write does not affect reads or later writes`() {
        underTest.write(url, "good")
        val payloadFileName = directory.list()!!.single()
        File(directory, "$payloadFileName.tmp").writeText("partial write from a crashed process")

        assertThat(underTest.read(url)).isEqualTo("good")
        assertThat(underTest.write(url, "newer")).isNotNull
        assertThat(underTest.read(url)).isEqualTo("newer")
    }

    @Test
    fun `write works again after clear`() {
        underTest.write(url, "payload")
        underTest.clear()

        assertThat(underTest.write(url, "again")).isNotNull
        assertThat(underTest.read(url)).isEqualTo("again")
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
    fun `write returns null when the directory cannot be used`() {
        val blockedByFile = ETagPayloadStore(temporaryFolder.newFile())

        assertThat(blockedByFile.write(url, "payload")).isNull()
        assertThat(blockedByFile.read(url)).isNull()
    }

    @Test
    fun `an empty payload round-trips with a zero size`() {
        val sizeBytes = underTest.write(url, "")

        assertThat(sizeBytes).isEqualTo(0L)
        assertThat(underTest.read(url, expectedSizeBytes = 0L)).isEqualTo("")

        underTest.clear()
        // A missing file also has length 0: the size check alone must not turn it into a hit.
        assertThat(underTest.read(url, expectedSizeBytes = 0L)).isNull()
    }

    @Test
    fun `a payload the encoder cannot represent fails the write instead of being altered`() {
        val payloadWithUnpairedSurrogate = "{\"key\":\"\uD83C\"}"

        assertThat(underTest.write(url, payloadWithUnpairedSurrogate)).isNull()
        assertThat(underTest.read(url)).isNull()
    }

    @Test
    fun `a corrupt payload file reads back as a miss instead of garbage`() {
        underTest.write(url, "valid")
        val payloadFile = File(directory, directory.list()!!.single())
        payloadFile.writeBytes(byteArrayOf(0x7B, -1, -2, 0x22))

        assertThat(underTest.read(url)).isNull()
    }
}
