package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Checksum
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
        val expectedInfo = ETagPayloadInfo(
            sizeBytes = 0L,
            checksum = Checksum.generate(byteArrayOf(), Checksum.Algorithm.SHA256),
        )

        assertThat(underTest.read(url, expectedInfo)).isNull()
    }

    @Test
    fun `write and read round-trip a payload`() {
        val payload = "{\"offerings\":[{\"id\":\"premium\",\"desc\":\"a \\\"quoted\\\" name\"}]}\nwith\nnewlines"

        val info = underTest.write(url, payload)!!

        assertThat(info.sizeBytes).isEqualTo(payload.toByteArray().size.toLong())
        assertThat(info.checksum).isEqualTo(
            Checksum.generate(payload.toByteArray(), Checksum.Algorithm.SHA256),
        )
        assertThat(underTest.read(url, info)).isEqualTo(payload)
    }

    @Test
    fun `a truncated payload file reads back as a miss`() {
        val info = underTest.write(url, "the full payload")!!
        val payloadFile = File(directory, directory.list()!!.single())
        payloadFile.writeText("the full pay")

        assertThat(underTest.read(url, info)).isNull()
    }

    @Test
    fun `a same-length corrupt payload file reads back as a miss`() {
        val info = underTest.write(url, "the full payload")!!
        val payloadFile = File(directory, directory.list()!!.single())
        val corrupted = payloadFile.readBytes().apply {
            this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte()
        }
        payloadFile.writeBytes(corrupted)

        assertThat(payloadFile.length()).isEqualTo(info.sizeBytes)
        assertThat(underTest.read(url, info)).isNull()
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

        val info = underTest.write(url, payload)!!

        assertThat(info.sizeBytes).isEqualTo(payload.toByteArray().size.toLong())
        assertThat(underTest.read(url, info)).isEqualTo(payload)
    }

    @Test
    fun `write overwrites the previous payload for the same url`() {
        underTest.write(url, "first")
        val secondInfo = underTest.write(url, "second")!!

        assertThat(underTest.read(url, secondInfo)).isEqualTo("second")
    }

    @Test
    fun `payloads for different urls do not collide`() {
        val otherUrl = "$url#rc_payload"
        val firstInfo = underTest.write(url, "one")!!
        val secondInfo = underTest.write(otherUrl, "two")!!

        assertThat(underTest.read(url, firstInfo)).isEqualTo("one")
        assertThat(underTest.read(otherUrl, secondInfo)).isEqualTo("two")
    }

    @Test
    fun `clear removes all payloads`() {
        val info = underTest.write(url, "payload")!!
        underTest.clear()

        assertThat(underTest.read(url, info)).isNull()
        assertThat(directory.exists()).isFalse
    }

    @Test
    fun `a leftover temp file from a crashed write does not affect reads or later writes`() {
        val originalInfo = underTest.write(url, "good")!!
        val payloadFileName = directory.list()!!.single()
        File(directory, "$payloadFileName.tmp").writeText("partial write from a crashed process")

        assertThat(underTest.read(url, originalInfo)).isEqualTo("good")
        val newerInfo = underTest.write(url, "newer")!!
        assertThat(underTest.read(url, newerInfo)).isEqualTo("newer")
    }

    @Test
    fun `write works again after clear`() {
        underTest.write(url, "payload")
        underTest.clear()

        val info = underTest.write(url, "again")!!
        assertThat(underTest.read(url, info)).isEqualTo("again")
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
        val originalInfo = underTest.write(url, "original")!!
        val payloadFileName = directory.list()!!.single()
        val blockingDirectory = File(directory, payloadFileName)
        blockingDirectory.deleteRecursively()
        File(blockingDirectory, "child").apply { parentFile!!.mkdirs(); writeText("blocks the rename") }

        assertThat(underTest.write(url, "new payload")).isNull()
        assertThat(underTest.read(url, originalInfo)).isNull()
    }

    @Test
    fun `clear on a store that never wrote is a no-op`() {
        underTest.clear()

        val expectedInfo = ETagPayloadInfo(
            sizeBytes = 0L,
            checksum = Checksum.generate(byteArrayOf(), Checksum.Algorithm.SHA256),
        )
        assertThat(underTest.read(url, expectedInfo)).isNull()
    }

    @Test
    fun `write returns null when the directory cannot be used`() {
        val blockedByFile = ETagPayloadStore(temporaryFolder.newFile())

        assertThat(blockedByFile.write(url, "payload")).isNull()
        val expectedInfo = ETagPayloadInfo(
            sizeBytes = 0L,
            checksum = Checksum.generate(byteArrayOf(), Checksum.Algorithm.SHA256),
        )
        assertThat(blockedByFile.read(url, expectedInfo)).isNull()
    }

    @Test
    fun `an empty payload round-trips with a zero size`() {
        val info = underTest.write(url, "")!!

        assertThat(info.sizeBytes).isEqualTo(0L)
        assertThat(info.checksum).isEqualTo(
            Checksum.generate(byteArrayOf(), Checksum.Algorithm.SHA256),
        )
        assertThat(underTest.read(url, info)).isEqualTo("")

        underTest.clear()
        // A missing file also has length 0: the size check alone must not turn it into a hit.
        assertThat(underTest.read(url, info)).isNull()
    }

    @Test
    fun `a payload the encoder cannot represent fails the write instead of being altered`() {
        val payloadWithUnpairedSurrogate = "{\"key\":\"\uD83C\"}"

        assertThat(underTest.write(url, payloadWithUnpairedSurrogate)).isNull()
    }

    @Test
    fun `a corrupt payload file reads back as a miss instead of garbage`() {
        val info = underTest.write(url, "valid")!!
        val payloadFile = File(directory, directory.list()!!.single())
        payloadFile.writeBytes(byteArrayOf(0x7B, -1, -2, 0x22))

        assertThat(underTest.read(url, info)).isNull()
    }
}
