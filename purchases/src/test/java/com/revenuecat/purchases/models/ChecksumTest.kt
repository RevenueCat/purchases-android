package com.revenuecat.purchases.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ChecksumTest {

    @Test
    fun `generate checksum from data - SHA256`() {
        val data = "Hello, World!".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA256)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.SHA256)
        assertThat(checksum.value).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f")
    }

    @Test
    fun `generate checksum from data - MD5`() {
        val data = "Test content".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.MD5)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.MD5)
        // MD5 hash of "Test content"
        assertThat(checksum.value).hasSize(32) // MD5 produces 32 hex chars
        assertThat(checksum.value).matches("[a-f0-9]{32}")
    }

    @Test
    fun `generate checksum from data - SHA384`() {
        val data = "Test".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA384)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.SHA384)
        assertThat(checksum.value).hasSize(96) // SHA384 produces 96 hex chars
    }

    @Test
    fun `generate checksum from data - SHA512`() {
        val data = "Test".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA512)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.SHA512)
        assertThat(checksum.value).hasSize(128) // SHA512 produces 128 hex chars
    }

    @Test
    fun `generate checksum from file - streaming`() {
        val file = File.createTempFile("test", ".tmp")
        try {
            file.writeBytes("Test content".toByteArray())

            val checksum = Checksum.generate(file, Checksum.Algorithm.MD5)

            assertThat(checksum.value).hasSize(32) // MD5 produces 32 hex chars
            assertThat(checksum.value).matches("[a-f0-9]{32}")
        } finally {
            file.delete()
        }
    }

    @Test
    fun `generate checksum from large file - memory efficient`() {
        val file = File.createTempFile("large", ".tmp")
        try {
            // Create 1MB file
            val largeData = ByteArray(1024 * 1024) { it.toByte() }
            file.writeBytes(largeData)

            val checksum = Checksum.generate(file, Checksum.Algorithm.SHA256)

            assertThat(checksum.value).isNotEmpty()
            assertThat(checksum.value).hasSize(64) // SHA256 produces 64 hex chars
        } finally {
            file.delete()
        }
    }

    @Test
    fun `generateAndConsume validates while streaming`() {
        val testData = "Test content".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val capturedChunks = mutableListOf<ByteArray>()

        val checksum = Checksum.generateAndConsume(
            inputStream,
            Checksum.Algorithm.MD5
        ) { buffer, bytesRead ->
            capturedChunks.add(buffer.copyOfRange(0, bytesRead))
        }

        assertThat(checksum.value).hasSize(32) // MD5 produces 32 hex chars
        assertThat(checksum.value).matches("[a-f0-9]{32}")
        assertThat(capturedChunks).isNotEmpty()
    }

    @Test
    fun `compare matching checksums - succeeds`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "abc123")

        // Should not throw
        checksum1.compare(checksum2)
    }

    @Test
    fun `compare matching checksums - case insensitive`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "ABC123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "abc123")

        // Should not throw
        checksum1.compare(checksum2)
    }

    @Test
    fun `compare mismatched checksums - throws`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "def456")

        val exception = assertThrows<Checksum.ChecksumValidationException> {
            checksum1.compare(checksum2)
        }

        assertThat(exception.message).contains("Checksum mismatch")
        assertThat(exception.message).contains("expected def456")
        assertThat(exception.message).contains("got abc123")
    }

    @Test
    fun `compare mismatched algorithms - throws`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.MD5, "abc123")

        val exception = assertThrows<Checksum.ChecksumValidationException> {
            checksum1.compare(checksum2)
        }

        assertThat(exception.message).contains("Algorithm mismatch")
        assertThat(exception.message).contains("expected MD5")
        assertThat(exception.message).contains("got SHA256")
    }

    @Test
    fun `Algorithm fromString - valid values`() {
        assertThat(Checksum.Algorithm.fromString("sha256")).isEqualTo(Checksum.Algorithm.SHA256)
        assertThat(Checksum.Algorithm.fromString("SHA256")).isEqualTo(Checksum.Algorithm.SHA256)
        assertThat(Checksum.Algorithm.fromString("sha384")).isEqualTo(Checksum.Algorithm.SHA384)
        assertThat(Checksum.Algorithm.fromString("sha512")).isEqualTo(Checksum.Algorithm.SHA512)
        assertThat(Checksum.Algorithm.fromString("md5")).isEqualTo(Checksum.Algorithm.MD5)
        assertThat(Checksum.Algorithm.fromString("MD5")).isEqualTo(Checksum.Algorithm.MD5)
    }

    @Test
    fun `Algorithm fromString - invalid value returns null`() {
        assertThat(Checksum.Algorithm.fromString("invalid")).isNull()
        assertThat(Checksum.Algorithm.fromString("")).isNull()
        assertThat(Checksum.Algorithm.fromString("sha1")).isNull()
    }

    @Test
    fun `generate empty data produces valid checksum`() {
        val data = ByteArray(0)
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA256)

        assertThat(checksum.value).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    fun `same data produces same checksum consistently`() {
        val data = "Consistent test data".toByteArray()

        val checksum1 = Checksum.generate(data, Checksum.Algorithm.SHA256)
        val checksum2 = Checksum.generate(data, Checksum.Algorithm.SHA256)

        assertThat(checksum1.value).isEqualTo(checksum2.value)
    }

    @Test
    fun `different data produces different checksums`() {
        val data1 = "Data 1".toByteArray()
        val data2 = "Data 2".toByteArray()

        val checksum1 = Checksum.generate(data1, Checksum.Algorithm.SHA256)
        val checksum2 = Checksum.generate(data2, Checksum.Algorithm.SHA256)

        assertThat(checksum1.value).isNotEqualTo(checksum2.value)
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}", e)
        }
    }
}
