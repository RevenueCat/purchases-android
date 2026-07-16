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

        assertThat(underTest.write(url, payload)).isTrue
        assertThat(underTest.read(url)).isEqualTo(payload)
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

        assertThat(underTest.write(url, payload)).isTrue
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
    fun `write works again after clear`() {
        underTest.write(url, "payload")
        underTest.clear()

        assertThat(underTest.write(url, "again")).isTrue
        assertThat(underTest.read(url)).isEqualTo("again")
    }
}
