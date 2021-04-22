package com.revenuecat.purchases.common.networking

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.filterNotNullValues
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ETagManagerTest {

    private val mockedPrefs = mockk<SharedPreferences>()
    private val underTest = ETagManager(mockedPrefs)
    private val slotSharedPreferencesKey = slot<String>()
    private val slotSharedPreferencesValue = slot<String>()
    private val mockEditor = mockk<SharedPreferences.Editor>()

    @Before
    fun setup() {
        every {
            mockedPrefs.edit()
        } returns mockEditor
        every {
            mockEditor.putString(capture(slotSharedPreferencesKey), capture(slotSharedPreferencesValue))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just Runs
        every {
            mockEditor.clear()
        } returns mockEditor
    }

    @Test
    fun `An empty ETag header is added if there is no ETag saved for that request`() {
        val httpRequestWithoutETagHeader = getHTTPRequest(eTag = null)

        mockCachedHTTPResult(httpRequestWithoutETagHeader, null)

        val requestWithETagHeader = underTest.getETagHeader(httpRequestWithoutETagHeader)
        val eTagHeader = requestWithETagHeader.headers[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isBlank()
    }

    @Test
    fun `An ETag header is added if there is an ETag saved for that request`() {
        val httpRequestWithoutETagHeader = getHTTPRequest(eTag = null)
        val expectedETag = "etag"

        mockCachedHTTPResult(httpRequestWithoutETagHeader, expectedETag)

        val requestWithETagHeader = underTest.getETagHeader(httpRequestWithoutETagHeader)
        val eTagHeader = requestWithETagHeader.headers[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `If response code is 304, use cached result`() {
        val eTag = "eTag"
        val mockedConnection = mockURLConnectionETag(eTag)
        val httpRequest = getHTTPRequest(eTag = eTag)

        val cachedHTTPResult = mockCachedHTTPResult(httpRequest, eTag)!!.httpResult

        val processedResponse = underTest.retrieveResponsePayload(httpRequest, mockedConnection, HTTPResult(304, ""))

        assertThat(processedResponse).isNotNull
        assertThat(processedResponse!!.payload).isEqualTo(cachedHTTPResult.payload)
        assertThat(processedResponse.responseCode).isEqualTo(cachedHTTPResult.responseCode)
    }

    @Test
    fun `If response code is not 304, don't return cached result and return result from backend`() {
        val eTag = "eTag"
        val newETag = "new_eTag"
        val mockedConnection = mockURLConnectionETag(newETag)
        val httpRequest = getHTTPRequest(eTag = eTag)

        val cachedHTTPResult = mockCachedHTTPResult(httpRequest, eTag)
        assertThat(cachedHTTPResult).isNotNull

        val resultFromBackend = HTTPResult(200, Responses.validEmptyPurchaserResponse)
        val processedResponse = underTest.retrieveResponsePayload(httpRequest, mockedConnection, resultFromBackend)

        assertThat(processedResponse).isNotNull
        assertThat(processedResponse!!.payload).isEqualTo(resultFromBackend.payload)
        assertThat(processedResponse.responseCode).isEqualTo(resultFromBackend.responseCode)
    }

    @Test
    fun `If response code is not 304, store response in cache`() {
        val eTag = "eTag"
        val newETag = "new_eTag"
        val mockedConnection = mockURLConnectionETag(newETag)
        val httpRequest = getHTTPRequest(eTag = eTag)

        val httpRequestHash = underTest.getOrCalculateAndSaveHTTPRequestHash(httpRequest)
        val resultFromBackend = HTTPResult(200, Responses.validEmptyPurchaserResponse)

        underTest.retrieveResponsePayload(httpRequest, mockedConnection, resultFromBackend)

        val expectedCachedResult = HTTPResultWithETag(newETag, resultFromBackend)

        assertThat(slotSharedPreferencesKey.isCaptured).isTrue()
        assertThat(slotSharedPreferencesValue.isCaptured).isTrue()

        assertThat(slotSharedPreferencesKey.captured).isEqualTo(httpRequestHash)
        assertThat(slotSharedPreferencesValue.captured).isEqualTo(expectedCachedResult.serialize())
    }

    @Test
    fun `If response code is 500, don't store response in cache`() {
        val eTag = "eTag"
        val newETag = "new_eTag"
        val mockedConnection = mockURLConnectionETag(newETag)
        val httpRequest = getHTTPRequest(eTag = eTag)

        underTest.getOrCalculateAndSaveHTTPRequestHash(httpRequest)
        val resultFromBackend = HTTPResult(500, Responses.validEmptyPurchaserResponse)

        underTest.retrieveResponsePayload(httpRequest, mockedConnection, resultFromBackend)

        HTTPResultWithETag(newETag, resultFromBackend)

        assertThat(slotSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotSharedPreferencesValue.isCaptured).isFalse()

        verify(exactly = 0) {
            mockEditor.putString(any(), any())
        }
    }

    @Test
    fun `Clearing caches removes all shared preferences`() {
        underTest.clearCaches()

        verify {
            mockEditor.clear()
        }
    }

    @Test
    fun `HTTP Header is empty when refreshing etag`() {
        val httpRequestWithoutETagHeader = getHTTPRequest(eTag = null)
        val expectedETag = ""

        mockCachedHTTPResult(httpRequestWithoutETagHeader, expectedETag)

        val requestWithETagHeader = underTest.getETagHeader(httpRequestWithoutETagHeader, refreshETag = true)
        val eTagHeader = requestWithETagHeader.headers[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `If response code is 304, but there is no cached result, return null when processing the request`() {
        val eTag = "eTag"
        val mockedConnection = mockURLConnectionETag(eTag)
        val httpRequest = getHTTPRequest(eTag = eTag)

        val httpRequestHash = underTest.getOrCalculateAndSaveHTTPRequestHash(httpRequest)
        every {
            mockedPrefs.getString(httpRequestHash, null)
        } returns null

        val processedResponse = underTest.retrieveResponsePayload(httpRequest, mockedConnection, HTTPResult(304, ""))

        assertThat(processedResponse).isNull()
    }

    private fun mockURLConnectionETag(eTag: String): HttpURLConnection {
        val mockedConnection = mockk<HttpURLConnection>(relaxed = true).also {
            every { it.getHeaderField(ETAG_HEADER_NAME) } returns eTag
        }
        return mockedConnection
    }

    private fun mockCachedHTTPResult(
        httpRequestWithoutETagHeader: HTTPRequest,
        expectedETag: String?
    ): HTTPResultWithETag? {
        val cachedResult = expectedETag?.let {
            HTTPResultWithETag(expectedETag, HTTPResult(200, "{}"))
        }
        val httpRequestHash = underTest.getOrCalculateAndSaveHTTPRequestHash(httpRequestWithoutETagHeader)
        every {
            mockedPrefs.getString(httpRequestHash, null)
        } returns cachedResult?.serialize()
        return cachedResult
    }

    private fun getHTTPRequest(eTag: String?): HTTPRequest {
        val fullURL = URL("https://api.revenuecat.com/v1/subscribers/appUserID")
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-Platform" to "android",
            "X-Platform-Flavor" to "native",
            "X-Platform-Version" to "29",
            "X-Version" to "4.1.0",
            "X-Client-Locale" to "en-US",
            "X-Client-Version" to "1.0",
            "X-Observer-Mode-Enabled" to "false",
            "Authorization" to "Bearer apiKey",
            ETAG_HEADER_NAME to eTag
        ).filterNotNullValues()
        return HTTPRequest(fullURL, headers, body = null)
    }
}