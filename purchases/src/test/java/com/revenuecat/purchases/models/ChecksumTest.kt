package com.revenuecat.purchases.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

public class ChecksumTest {

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
        assertThat(checksum.value).isEqualTo("8bfa8e0684108f419933a5995264d150")
    }

    @Test
    fun `generate checksum from data - SHA384`() {
        val data = "Test".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA384)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.SHA384)
        assertThat(checksum.value)
            .isEqualTo(
                "7b8f4654076b80eb963911f19cfad1aaf4285ed48e826f6cde1b01a79aa73fadb5446e667fc4f90417782c91270540f3",
            )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `generate checksum from data - SHA512`() {
        val data = "Test".toByteArray()
        val checksum = Checksum.generate(data, Checksum.Algorithm.SHA512)

        assertThat(checksum.algorithm).isEqualTo(Checksum.Algorithm.SHA512)
        assertThat(checksum.value)
            .isEqualTo(
                "c6ee9e33cf5c6715a1d148fd73f7318884b41adcb916021e2bc0e800a5c5dd97f5142178f6ae88c8fdd98e1afb0ce4c8d2c54b5f37b30b7da1997bb33b0b8a31",
            )
    }

    @Test
    fun `compare matching checksums - succeeds`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "abc123")

        assert(checksum1 == checksum2)
    }

    @Test
    fun `compare matching checksums - case insensitive`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "ABC123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "abc123")

        assert(checksum1 == checksum2)
    }

    @Test
    fun `compare mismatched checksums - returns false`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "def456")

        assert(checksum1 != checksum2)
    }

    @Test
    fun `compare mismatched algorithms - returns false`() {
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val checksum2 = Checksum(Checksum.Algorithm.MD5, "abc123")

        assert(checksum1 != checksum2)
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
}
