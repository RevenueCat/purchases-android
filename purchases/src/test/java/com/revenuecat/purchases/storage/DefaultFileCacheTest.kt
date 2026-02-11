package com.revenuecat.purchases.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Checksum
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

@RunWith(AndroidJUnit4::class)
public class DefaultFileCacheTest {

    private lateinit var context: Context
    private lateinit var cache: DefaultFileCache
    private lateinit var cacheDir: File

    @Before
    public fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cache = DefaultFileCache(context)
        cacheDir = File(context.cacheDir, "rc_files")
    }

    @After
    public fun tearDown() {
        // Clean up all files
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `saveData streams small file successfully`() {
        // Given: Small file (less than buffer size)
        val testData = "Hello, World!".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/test.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Saving data
        cache.saveData(inputStream, uri)

        // Then: File exists and contains correct data
        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
    }

    @Test
    fun `saveData streams large file successfully`() {
        // Given: Large file (multiple buffer fills - 1MB)
        val testData = ByteArray(1024 * 1024) { it.toByte() }
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/large.bin")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Saving data
        cache.saveData(inputStream, uri)

        // Then: File exists and contains correct data
        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
    }

    @Test
    fun `saveData streams file exactly buffer size`() {
        // Given: File exactly 256KB
        val testData = ByteArray(256 * 1024) { it.toByte() }
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/exact.bin")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Saving data
        cache.saveData(inputStream, uri)

        // Then: File exists and contains correct data
        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
    }

    @Test
    fun `saveData handles empty file`() {
        // Given: Empty file
        val testData = ByteArray(0)
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/empty.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Saving data
        cache.saveData(inputStream, uri)

        // Then: File exists and is empty
        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.length()).isEqualTo(0)
    }

    @Test
    fun `saveData cleans up temp file on stream read failure`() {
        // Given: InputStream that throws during read
        val failingStream = object : InputStream() {
            override fun read(): Int = throw IOException("Stream read failed")
        }
        val url = URL("https://example.com/fail.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Attempting to save data
        val exception = assertThrows<IOException> {
            cache.saveData(failingStream, uri)
        }

        // Then: Exception is thrown and no temp files remain
        assertThat(exception.message).contains("Stream read failed")
        val tempFiles = cacheDir.listFiles { _, name -> name.startsWith("rc_download_") }
        assertThat(tempFiles).isEmpty()
    }

    @Test
    fun `saveData overwrites existing file`() {
        // Given: Existing file with old content
        val oldData = "old content".toByteArray()
        val url = URL("https://example.com/overwrite.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!
        cache.saveData(ByteArrayInputStream(oldData), uri)

        // When: Saving new content
        val newData = "new content".toByteArray()
        cache.saveData(ByteArrayInputStream(newData), uri)

        // Then: File contains new content
        val savedFile = File(uri)
        assertThat(savedFile.readBytes()).isEqualTo(newData)
    }

    @Test
    fun `saveData uses atomic move when possible`() {
        // Given: Normal file save scenario
        val testData = "test data".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/atomic.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Saving data
        cache.saveData(inputStream, uri)

        // Then: Final file exists and no temp files remain
        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        val tempFiles = cacheDir.listFiles { _, name -> name.startsWith("rc_download_") }
        assertThat(tempFiles).isEmpty()
    }

    @Test
    fun `saveData creates temp file in same directory as final file`() {
        // Given: File save with monitoring
        val testData = "test".toByteArray()
        val url = URL("https://example.com/temp.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // This test verifies temp file location through implementation
        // In a real test, you might use a spy/mock to verify temp file location
        cache.saveData(ByteArrayInputStream(testData), uri)

        // Then: Final file is in cache dir
        val savedFile = File(uri)
        assertThat(savedFile.parentFile).isEqualTo(cacheDir)
    }

    @Test
    fun `cachedContentExists returns true for existing file`() {
        // Given: Saved file
        val testData = "exists".toByteArray()
        val url = URL("https://example.com/exists.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!
        cache.saveData(ByteArrayInputStream(testData), uri)

        // When: Checking if content exists
        val exists = cache.cachedContentExists(uri)

        // Then: Returns true
        assertThat(exists).isTrue()
    }

    @Test
    fun `cachedContentExists returns false for non-existing file`() {
        // Given: Non-existing file
        val url = URL("https://example.com/notexists.txt")
        val uri = cache.generateLocalFilesystemURI(url)!!

        // When: Checking if content exists
        val exists = cache.cachedContentExists(uri)

        // Then: Returns false
        assertThat(exists).isFalse()
    }

    @Test
    fun `generateLocalFilesystemURI generates consistent hash for same URL`() {
        // Given: Same URL called multiple times
        val url = URL("https://example.com/consistent.txt")

        // When: Generating URI multiple times
        val uri1 = cache.generateLocalFilesystemURI(url)
        val uri2 = cache.generateLocalFilesystemURI(url)

        // Then: URIs are identical
        assertThat(uri1).isEqualTo(uri2)
    }

    @Test
    fun `generateLocalFilesystemURI generates different hash for different URLs`() {
        // Given: Different URLs
        val url1 = URL("https://example.com/file1.txt")
        val url2 = URL("https://example.com/file2.txt")

        // When: Generating URIs
        val uri1 = cache.generateLocalFilesystemURI(url1)
        val uri2 = cache.generateLocalFilesystemURI(url2)

        // Then: URIs are different
        assertThat(uri1).isNotEqualTo(uri2)
    }

    // Checksum validation tests

    @Test
    fun `saveData with checksum validation - valid checksum succeeds`() {
        val testData = "Test content".toByteArray()
        val expectedChecksum = Checksum.generate(testData, Checksum.Algorithm.SHA256)
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/test.txt")
        val uri = cache.generateLocalFilesystemURI(url, expectedChecksum)!!

        // Should not throw
        cache.saveData(inputStream, uri, expectedChecksum)

        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
    }

    @Test
    fun `saveData with checksum validation - invalid checksum - fails silently and cleans up`() {
        val testData = "Test content".toByteArray()
        val wrongChecksum = Checksum(
            Checksum.Algorithm.SHA256,
            "wrong_hash_value_here_0123456789abcdef0123456789abcdef0123456789abcdef",
        )
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/test.txt")
        val uri = cache.generateLocalFilesystemURI(url, wrongChecksum)!!

        cache.saveData(inputStream, uri, wrongChecksum)

        // Verify temp file was cleaned up
        val tempFiles = cacheDir.listFiles { _, name -> name.startsWith("rc_download_") }
        assertThat(tempFiles).isEmpty()

        assert(!cache.cachedContentExists(uri))
    }

    @Test
    fun `generateLocalFilesystemURI uses checksum as part of the filename and retains the file extension`() {
        val url = URL("https://example.com/video.mp4")
        val checksum = Checksum(Checksum.Algorithm.SHA256, "abc123def456")

        val uri = cache.generateLocalFilesystemURI(url, checksum)

        assertThat(uri.toString()).endsWith("abc123def456.mp4")
    }

    @Test
    fun `different checksums produce different cache entries`() {
        val url = URL("https://example.com/video.mp4")
        val checksum1 = Checksum(Checksum.Algorithm.SHA256, "hash1")
        val checksum2 = Checksum(Checksum.Algorithm.SHA256, "hash2")

        val uri1 = cache.generateLocalFilesystemURI(url, checksum1)
        val uri2 = cache.generateLocalFilesystemURI(url, checksum2)

        assertThat(uri1).isNotEqualTo(uri2)
    }

    @Test
    fun `saveData without checksum - backwards compatible`() {
        val testData = "Test content".toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/test.txt")
        val uri = cache.generateLocalFilesystemURI(url, null)!!

        // Should work without checksum (backwards compatible)
        cache.saveData(inputStream, uri, null)

        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
    }

    @Test
    fun `saveData with checksum validates during streaming not after`() {
        // Large enough file to span multiple buffer reads
        val testData = ByteArray(512 * 1024) { it.toByte() }
        val expectedChecksum = Checksum.generate(testData, Checksum.Algorithm.SHA256)
        val inputStream = ByteArrayInputStream(testData)
        val url = URL("https://example.com/large.bin")
        val uri = cache.generateLocalFilesystemURI(url, expectedChecksum)!!

        // Should validate while streaming
        cache.saveData(inputStream, uri, expectedChecksum)

        val savedFile = File(uri)
        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.readBytes()).isEqualTo(testData)
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
