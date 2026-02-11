package com.revenuecat.purchases.paywalls

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import com.revenuecat.purchases.utils.TestUrlConnection
import com.revenuecat.purchases.utils.TestUrlConnectionFactory
import io.mockk.every
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
public class FontLoaderTest {

    public companion object {
        private const val FONT_URL = "https://example.com/font.ttf"
        private const val FONT_FILE_CONTENT = "Test Font Content"
    }

    private val fontFileContentMd5 = calculateMd5()
    private val fontInfo = createFontInfo()

    private lateinit var mockContext: Context
    private lateinit var mockCacheDir: File
    private lateinit var urlConnectionFactory: TestUrlConnectionFactory
    private lateinit var testScope: TestScope
    private lateinit var fontLoader: FontLoader

    @Before
    public fun setUp() {
        mockContext = mockk<Context>()
        mockCacheDir = File("test_font_loader_cache").apply { mkdirs() }
        testScope = TestScope()

        createFontLoader()
    }

    @After
    public fun tearDown() {
        mockCacheDir.deleteRecursively()
    }

    @Test
    fun `getCachedFontFileOrStartDownload returns null and starts download when file not cached`() = runTest {
        val result = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)

        // Should return null immediately since download is async
        assertNull(result)

        // Wait for download to complete
        testScope.advanceUntilIdle()

        // Now the file should be cached
        verifyDownloadedFontFamily(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))

        // Verify the URL connection was created only once
        assertEquals(urlConnectionFactory.createdConnections.size, 1)
        assertEquals(urlConnectionFactory.createdConnections.first(), FONT_URL)
    }

    @Test
    fun `getCachedFontFileOrStartDownload handles HTTP error`() = runTest {
        createFontLoader(testUrlConnections = mapOf(
            FONT_URL to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_NOT_FOUND,
                inputStream = ByteArrayInputStream(ByteArray(0))
            )
        ))

        val result = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
        assertNull(result)

        // Wait for download attempt to complete
        testScope.advanceUntilIdle()

        // File should not be cached
        val result2 = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
        assertNull(result2)

        testScope.advanceUntilIdle()

        assertEquals(urlConnectionFactory.createdConnections.size, 2)
        for (createdConnection in urlConnectionFactory.createdConnections) {
            assertEquals(createdConnection, FONT_URL)
        }
    }

    @Test
    fun `getCachedFontFileOrStartDownload handles MD5 mismatch`() = runTest {
        val fontInfoWithInvalidHash = createFontInfo(hash = "invalid_hash")
        val result = fontLoader.getCachedFontFamilyOrStartDownload(fontInfoWithInvalidHash)
        assertNull(result)

        // Wait for download attempt to complete
        testScope.advanceUntilIdle()

        // File should not be cached due to MD5 mismatch
        val cachedFile = fontLoader.getCachedFontFamilyOrStartDownload(fontInfoWithInvalidHash)
        assertNull(cachedFile)
    }

    @Test
    fun `getCachedFontFileOrStartDownload does not download same font multiple times at the same time`() = runTest {
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))

        testScope.advanceUntilIdle()

        verifyDownloadedFontFamily(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))

        assertEquals(urlConnectionFactory.createdConnections.size, 1)
        assertEquals(urlConnectionFactory.createdConnections.first(), FONT_URL)
    }

    @Test
    fun `getCachedFontFileOrStartDownload adds fonts to existing font family`() = runTest {
        val url2 = "https://example.com/font2.ttf"
        val font2Content = "Font 2 Content"
        createFontLoader(testUrlConnections = mapOf(
            FONT_URL to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_OK,
                inputStream = ByteArrayInputStream(FONT_FILE_CONTENT.toByteArray())
            ),
            url2 to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_OK,
                inputStream = ByteArrayInputStream(font2Content.toByteArray())
            )
        ))
        val fontToAdd = createFontInfo(
            url = "https://example.com/font2.ttf",
            hash = calculateMd5(font2Content),
            weight = 700,
            style = FontStyle.NORMAL,
        )
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))

        testScope.advanceUntilIdle()

        verifyDownloadedFontFamily(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontToAdd))

        testScope.advanceUntilIdle()

        val fontFamily = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
        val fontFamily2 = fontLoader.getCachedFontFamilyOrStartDownload(fontToAdd)
        assertEquals(fontFamily, fontFamily2)
        verifyDownloadedFontFamily(
            fontFamily = fontFamily,
            expectedFonts = listOf(
                FontToVerify(400, FontStyle.ITALIC, FONT_FILE_CONTENT),
                FontToVerify(700, FontStyle.NORMAL, font2Content),
            )
        )

        assertEquals(urlConnectionFactory.createdConnections.size, 2)
        assertEquals(urlConnectionFactory.createdConnections[0], FONT_URL)
        assertEquals(urlConnectionFactory.createdConnections[1], url2)
    }

    @Test
    fun `getCachedFontFileOrStartDownload does not add fonts if font family is different`() = runTest {
        val url2 = "https://example.com/font2.ttf"
        val font2Content = "Font 2 Content"
        createFontLoader(testUrlConnections = mapOf(
            FONT_URL to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_OK,
                inputStream = ByteArrayInputStream(FONT_FILE_CONTENT.toByteArray())
            ),
            url2 to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_OK,
                inputStream = ByteArrayInputStream(font2Content.toByteArray())
            )
        ))
        val differentFamilyFont = createFontInfo(
            family = "DifferentFontFamily",
            url = "https://example.com/font2.ttf",
            hash = calculateMd5(font2Content),
            weight = 700,
            style = FontStyle.NORMAL,
        )
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))
        assertNull(fontLoader.getCachedFontFamilyOrStartDownload(differentFamilyFont))

        testScope.advanceUntilIdle()

        verifyDownloadedFontFamily(fontLoader.getCachedFontFamilyOrStartDownload(fontInfo))
        verifyDownloadedFontFamily(
            fontFamily = fontLoader.getCachedFontFamilyOrStartDownload(differentFamilyFont),
            expectedFamily = "DifferentFontFamily",
            expectedFonts = listOf(
                FontToVerify(700, FontStyle.NORMAL, font2Content)
            )
        )
    }

    @Test
    fun `getCachedFontFileOrStartDownload handles null cacheDir gracefully`() = runTest {
        val contextWithNullCacheDir = mockk<Context> {
            every { cacheDir } returns null
        }
        urlConnectionFactory = TestUrlConnectionFactory(
            mockedConnections = mapOf(
                FONT_URL to TestUrlConnection(
                    responseCode = HttpURLConnection.HTTP_OK,
                    inputStream = ByteArrayInputStream(FONT_FILE_CONTENT.toByteArray())
                )
            ),
        )
        fontLoader = FontLoader(
            context = contextWithNullCacheDir,
            providedCacheDir = null,
            ioScope = testScope,
            urlConnectionFactory = urlConnectionFactory,
        )

        // Should not crash, just return null and not attempt download
        val result = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
        assertNull(result)

        testScope.advanceUntilIdle()

        // Should still be null since no download could happen
        val result2 = fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
        assertNull(result2)

        // No connections should have been created since cacheDir was null
        assertEquals(0, urlConnectionFactory.createdConnections.size)
    }

    private fun createFontInfo(
        value: String = "TestFont",
        url: String = FONT_URL,
        hash: String = calculateMd5(),
        family: String = "TestFontFamily",
        weight: Int = 400,
        style: FontStyle = FontStyle.ITALIC,
    ): FontInfo.Name = FontInfo.Name(
        value = value,
        url = url,
        hash = hash,
        family = family,
        weight = weight,
        style = style
    )

    private data class FontToVerify(
        val weight: Int,
        val style: FontStyle,
        val content: String = FONT_FILE_CONTENT
    )

    private fun verifyDownloadedFontFamily(
        fontFamily: DownloadedFontFamily?,
        expectedFamily: String = "TestFontFamily",
        expectedFonts: List<FontToVerify> = listOf(
            FontToVerify(weight = 400, style = FontStyle.ITALIC, content = FONT_FILE_CONTENT)
        ),
    ) {
        assertNotNull(fontFamily)
        assertEquals(expectedFamily, fontFamily!!.family)
        assertEquals(expectedFonts.size, fontFamily.fonts.size)
        for ((index, expectedFont) in expectedFonts.withIndex()) {
            val font = fontFamily.fonts[index]
            assertEquals(expectedFont.weight, font.weight)
            assertEquals(expectedFont.style, font.style)
            assertEquals(expectedFont.content, font.file.readText())
        }
    }

    private fun createFontLoader(
        testUrlConnections: Map<String, TestUrlConnection> = mapOf(
            FONT_URL to TestUrlConnection(
                responseCode = HttpURLConnection.HTTP_OK,
                inputStream = ByteArrayInputStream(FONT_FILE_CONTENT.toByteArray())
            )
        )
    ) {
        urlConnectionFactory = TestUrlConnectionFactory(
            mockedConnections = testUrlConnections,
        )

        fontLoader = FontLoader(
            context = mockContext,
            providedCacheDir = mockCacheDir,
            ioScope = testScope,
            urlConnectionFactory = urlConnectionFactory,
        )
    }

    private fun calculateMd5(fileContent: String = FONT_FILE_CONTENT): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(fileContent.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
