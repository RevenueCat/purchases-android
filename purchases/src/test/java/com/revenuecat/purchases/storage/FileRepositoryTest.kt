package com.revenuecat.purchases.storage

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.utils.CoroutineTest
import com.revenuecat.purchases.utils.TestUrlConnection
import com.revenuecat.purchases.utils.TestUrlConnectionFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
class FileRepositoryTest : CoroutineTest() {
    private companion object {
        const val TEST_URL = "https://www.sample.com"
        const val TEST_URI = "data:sample/some/path"
        val cacheUri = URI(TEST_URI)
        val url: URL = URL(TEST_URL)
        val goodConnection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream(ByteArray(0)),
        )

        val badConnection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_BAD_REQUEST,
            inputStream = ByteArrayInputStream(ByteArray(0)),
        )
    }

    @Test
    fun `if content exists, network is not called`() = runTest {
        val factory = TestUrlConnectionFactory(emptyMap())

        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns true

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        val result = defaultFileRepository.generateOrGetCachedFileURL(url)

        assertThat(result).isEqualTo(cacheUri)
        assertThat(factory.createdConnections.size).isEqualTo(0)
    }

    @Test
    fun `prefetch invokes network and saves file`() = runTest {
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to goodConnection))
        val mockCache = mockk<LocalFileCache>()

        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every { mockCache.saveData(any<InputStream>(), cacheUri) } just Runs

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            ioScope = this,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        defaultFileRepository.prefetch(listOf(url))

        advanceUntilIdle()

        defaultFileRepository.store.deferred.values.awaitAll()

        assertThat(factory.createdConnections.size).isEqualTo(1)
        assertThat(factory.createdConnections.first()).isEqualTo(TEST_URL)
        verify(exactly = 1) { mockCache.saveData(any<InputStream>(), cacheUri) }
    }

    @Test
    fun `when cacheUri cannot be assembled, throws exception`() = runTest {
        val factory = TestUrlConnectionFactory(emptyMap())

        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns null

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        assertThrows(DefaultFileRepository.Error.FailedToCreateCacheDirectory::class) {
            defaultFileRepository.generateOrGetCachedFileURL(url)
        }
    }

    @Test
    fun `when network fails, throws exception`() = runTest {
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to badConnection))
        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        assertThrows(DefaultFileRepository.Error.FailedToFetchFileFromRemoteSource::class) {
            defaultFileRepository.generateOrGetCachedFileURL(url)
        }

        assertThat(factory.createdConnections.size).isEqualTo(1)
        assertThat(factory.createdConnections.first()).isEqualTo(TEST_URL)
    }

    @Test
    fun `when network succeeds, saves file, returns uri`() = runTest {
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to goodConnection))
        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every { mockCache.saveData(any<InputStream>(), cacheUri) } just Runs

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        val result = defaultFileRepository.generateOrGetCachedFileURL(url)
        assertThat(result).isEqualTo(cacheUri)
        verify(exactly = 1) { mockCache.saveData(any<InputStream>(), cacheUri) }
    }

    @Test
    fun `when save fails, throws exception`() = runTest {
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to goodConnection))
        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every {
            mockCache.saveData(any<InputStream>(), cacheUri)
        } throws IOException("Failed to save data")

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        assertThrows(DefaultFileRepository.Error.FailedToSaveCachedFile::class) {
            defaultFileRepository.generateOrGetCachedFileURL(url)
        }

        verify(exactly = 1) { mockCache.saveData(any<InputStream>(), cacheUri) }
    }

    @Test
    fun `connection is closed after successful save`() = runTest {
        val connection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream("test data".toByteArray()),
        )
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to connection))
        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every { mockCache.saveData(any<InputStream>(), cacheUri) } just Runs

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        defaultFileRepository.generateOrGetCachedFileURL(url)

        // Verify connection was disconnected
        assertThat(connection.isDisconnected).isTrue()
    }

    @Test
    fun `connection is closed even when save fails`() = runTest {
        val connection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream("test data".toByteArray()),
        )
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to connection))
        val mockCache = mockk<LocalFileCache>()
        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every {
            mockCache.saveData(any<InputStream>(), cacheUri)
        } throws IOException("Save failed")

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        assertThrows(DefaultFileRepository.Error.FailedToSaveCachedFile::class) {
            defaultFileRepository.generateOrGetCachedFileURL(url)
        }

        // Verify connection was disconnected even on failure
        assertThat(connection.isDisconnected).isTrue()
    }

    @Test
    fun `stream is passed to saveData with correct content`() = runTest {
        val testData = "test file content".toByteArray()
        val connection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream(testData),
        )
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to connection))
        val mockCache = mockk<LocalFileCache>()
        val streamSlot = slot<InputStream>()

        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every { mockCache.saveData(capture(streamSlot), cacheUri) } answers {
            // Verify stream content
            val capturedStream = streamSlot.captured
            val readData = capturedStream.readBytes()
            assertThat(readData).isEqualTo(testData)
        }

        val defaultFileRepository = DefaultFileRepository(
            store = KeyedDeferredValueStore(),
            fileCacheManager = mockCache,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        defaultFileRepository.generateOrGetCachedFileURL(url)

        verify(exactly = 1) { mockCache.saveData(any<InputStream>(), cacheUri) }
    }

    @Test
    fun `concurrent requests for same URL only download once`() = runTest {
        val connection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream("data".toByteArray()),
        )
        val factory = TestUrlConnectionFactory(mapOf(TEST_URL to connection))
        val mockCache = mockk<LocalFileCache>()

        every { mockCache.generateLocalFilesystemURI(url) } returns cacheUri
        every { mockCache.cachedContentExists(cacheUri) } returns false
        every { mockCache.saveData(any<InputStream>(), cacheUri) } just Runs

        val store = KeyedDeferredValueStore<URL, URI>()
        val defaultFileRepository = DefaultFileRepository(
            store = store,
            fileCacheManager = mockCache,
            ioScope = this,
            logHandler = mockk<LogHandler>(relaxed = true),
            urlConnectionFactory = factory,
        )

        // Launch multiple concurrent requests for same URL
        val deferred1 = async { defaultFileRepository.generateOrGetCachedFileURL(url) }
        val deferred2 = async { defaultFileRepository.generateOrGetCachedFileURL(url) }
        val deferred3 = async { defaultFileRepository.generateOrGetCachedFileURL(url) }

        val results = awaitAll(deferred1, deferred2, deferred3)

        // All return same URI
        assertThat(results).allMatch { it == cacheUri }

        // Network was only called once
        assertThat(factory.createdConnections.size).isEqualTo(1)
        verify(exactly = 1) { mockCache.saveData(any<InputStream>(), cacheUri) }
    }
}
