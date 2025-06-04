package com.revenuecat.purchases.paywalls

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.TestUrlConnection
import com.revenuecat.purchases.utils.TestUrlConnectionFactory
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FontLoaderTest {

    companion object {
        private const val FONT_URL = "https://example.com/font.ttf"
        private const val FONT_FILE_CONTENT = "Test Font Content"
    }

    private val fontFileContentMd5 = calculateMd5()

    private lateinit var mockContext: Context
    private lateinit var mockCacheDir: File
    private lateinit var urlConnectionFactory: TestUrlConnectionFactory
    private lateinit var testScope: TestScope
    private lateinit var fontLoader: FontLoader

    @Before
    fun setUp() {
        mockContext = mockk<Context>()
        mockCacheDir = File("test_font_loader_cache").apply { mkdirs() }
        testScope = TestScope()

        val mockConnection = TestUrlConnection(
            responseCode = HttpURLConnection.HTTP_OK,
            inputStream = ByteArrayInputStream(FONT_FILE_CONTENT.toByteArray()),
        )

        createFontLoader(mockConnection)
    }

    @After
    fun tearDown() {
        mockCacheDir.deleteRecursively()
    }

    // TODO: Add back tests
//    @Test
//    fun `getCachedFontFileOrStartDownload returns null and starts download when file not cached`() = runTest {
//        val result = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//
//        // Should return null immediately since download is async
//        assertNull(result)
//
//        // Wait for download to complete
//        testScope.advanceUntilIdle()
//
//        // Now the file should be cached
//        val cachedFile = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//        assertNotNull(cachedFile)
//        assertEquals(FONT_FILE_CONTENT, cachedFile?.readText())
//    }
//
//    @Test
//    fun `getCachedFontFileOrStartDownload returns cached file immediately if it exists`() = runTest {
//        // First download
//        fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//        testScope.advanceUntilIdle()
//
//        // Second request should return cached file immediately
//        val cachedFile = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//        assertNotNull(cachedFile)
//        assertEquals(FONT_FILE_CONTENT, cachedFile?.readText())
//    }
//
//    @Test
//    fun `getCachedFontFileOrStartDownload handles HTTP error`() = runTest {
//        createFontLoader(TestUrlConnection(
//            responseCode = HttpURLConnection.HTTP_NOT_FOUND,
//            inputStream = ByteArrayInputStream(ByteArray(0))
//        ))
//
//        val result = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//        assertNull(result)
//
//        // Wait for download attempt to complete
//        testScope.advanceUntilIdle()
//
//        // File should not be cached
//        val cachedFile = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, fontFileContentMd5)
//        assertNull(cachedFile)
//    }
//
//    @Test
//    fun `getCachedFontFileOrStartDownload handles MD5 mismatch`() = runTest {
//        val result = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, "invalid-md5")
//        assertNull(result)
//
//        // Wait for download attempt to complete
//        testScope.advanceUntilIdle()
//
//        // File should not be cached due to MD5 mismatch
//        val cachedFile = fontLoader.getCachedFontFamilyOrStartDownload(FONT_URL, "invalid-md5")
//        assertNull(cachedFile)
//    }

    private fun createFontLoader(
        testUrlConnection: TestUrlConnection,
    ) {
        urlConnectionFactory = TestUrlConnectionFactory(
            mockedConnections = mapOf(FONT_URL to testUrlConnection)
        )

        fontLoader = FontLoader(
            context = mockContext,
            cacheDir = mockCacheDir,
            ioScope = testScope,
            urlConnectionFactory = urlConnectionFactory,
        )
    }

    private fun calculateMd5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(FONT_FILE_CONTENT.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
