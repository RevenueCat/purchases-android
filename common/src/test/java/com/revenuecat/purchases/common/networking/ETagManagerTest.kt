package com.revenuecat.purchases.common.networking

import android.content.SharedPreferences
import com.revenuecat.purchases.utils.LogMockExtension
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.filterNotNullValues
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URL

@ExtendWith(LogMockExtension::class)
class ETagManagerTest {

    private val mockedPrefs = mockk<SharedPreferences>()
    private val underTest = ETagManager(mockedPrefs)
    private val slotPutStringSharedPreferencesKey = slot<String>()
    private val slotPutSharedPreferencesValue = slot<String>()
    private val mockEditor = mockk<SharedPreferences.Editor>()

    @BeforeEach
    fun setup() {
        every {
            mockedPrefs.edit()
        } returns mockEditor
        every {
            mockEditor.putString(capture(slotPutStringSharedPreferencesKey), capture(slotPutSharedPreferencesValue))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just Runs
        every {
            mockEditor.clear()
        } returns mockEditor
    }

    @Test
    fun `ETag header is empty added if there is no ETag saved for that request`() {
        val path = "/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, path = path)

        val requestWithETagHeader = underTest.getETagHeader(path)
        val eTagHeader = requestWithETagHeader[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isBlank()
    }

    @Test
    fun `An ETag header is added if there is an ETag saved for that request`() {
        val path = "/v1/subscribers/appUserID"
        val expectedETag = "etag"
        mockCachedHTTPResult(expectedETag, path)

        val requestWithETagHeader = underTest.getETagHeader(path)
        val eTagHeader = requestWithETagHeader[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `If response code is 304, cached version should be used`() {
        val shouldUse = underTest.shouldUseCachedVersion(RCHTTPStatusCodes.NOT_MODIFIED)

        assertThat(shouldUse).isTrue()
    }

    @Test
    fun `If response code is not 304, cached version should not be used`() {
        val shouldUse = underTest.shouldUseCachedVersion(RCHTTPStatusCodes.SUCCESS)

        assertThat(shouldUse).isFalse()
    }

    @Test
    fun `Cached result is returned when calling getStoredResult`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val cachedHTTPResult = mockCachedHTTPResult(eTag, path)!!.httpResult

        val storedResult = underTest.getStoredResult(path)

        assertThat(storedResult).isNotNull
        assertThat(storedResult!!.payload).isEqualTo(cachedHTTPResult.payload)
        assertThat(storedResult.responseCode).isEqualTo(cachedHTTPResult.responseCode)
    }

    @Test
    fun `GetStoredResult returns null if there's nothing cached`() {
        val path = "/v1/subscribers/appUserID"
        every {
            mockedPrefs.getString(path, null)
        } returns null
        val storedResult = underTest.getStoredResult(path)

        assertThat(storedResult).isNull()
    }

    @Test
    fun `If response code is 304, don't store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(RCHTTPStatusCodes.NOT_MODIFIED, "")

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse()
    }

    @Test
    fun `If response code is not 304, store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse)
        val resultFromBackendWithETag = HTTPResultWithETag(eTag, resultFromBackend)

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isTrue()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isTrue()

        assertThat(slotPutStringSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotPutSharedPreferencesValue.captured).isEqualTo(resultFromBackendWithETag.serialize())
    }

    @Test
    fun `If response code is 500, don't store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(500, "{}")

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse()
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
        val path = "/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, path = path)

        val requestWithETagHeader = underTest.getETagHeader(path, refreshETag = true)
        val eTagHeader = requestWithETagHeader[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isBlank()
    }

    @Test
    fun `if backend returns a 200, store result from backend`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = "{}"
        val eTagInResponse = "etagInResponse"

        underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = false
        )

        assertStoredResponse(path, eTagInResponse, responsePayload)
    }

    @Test
    fun `don't store result from backend if the response code is 304 and there is a cached result`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val expectedETag = "etag"
        mockCachedHTTPResult(expectedETag, path)

        underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = false
        )

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse()
    }

    @Test
    fun `if should use cached version, but there's nothing cached, return null when getting result and don't cache anything`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        every {
            mockedPrefs.getString(path, null)
        } returns null

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = false
        )

        assertThat(result).isNull()
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse()
    }

    @Test
    fun `if should use cached version, but there's nothing cached, use backend result and don't cache anything if etag is already being refreshed`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        every {
            mockedPrefs.getString(path, null)
        } returns null

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = true
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_MODIFIED)
        assertThat(result.payload).isEqualTo(responsePayload)
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse()
    }

    @Test
    fun `if it should not use the cached version, use and cache backend result`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = false
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payload).isEqualTo(responsePayload)

        assertStoredResponse(path, eTagInResponse, responsePayload)
    }

    @Test
    fun `if should not use cached version, and it's refreshing the etag, use and cache backend result`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            connection = mockURLConnectionETag(eTagInResponse),
            urlPathWithVersion = path,
            refreshETag = true
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payload).isEqualTo(responsePayload)

        assertStoredResponse(path, eTagInResponse, responsePayload)
    }

    private fun mockURLConnectionETag(eTag: String): HttpURLConnection {
        return mockk<HttpURLConnection>(relaxed = true).also {
            every { it.getHeaderField(ETAG_HEADER_NAME) } returns eTag
        }
    }

    private fun mockCachedHTTPResult(
        expectedETag: String?,
        path: String
    ): HTTPResultWithETag? {
        val cachedResult = expectedETag?.let {
            HTTPResultWithETag(expectedETag, HTTPResult(RCHTTPStatusCodes.SUCCESS, "{}"))
        }
        every {
            mockedPrefs.getString(path, null)
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

    private fun assertStoredResponse(
        path: String,
        eTagInResponse: String,
        responsePayload: String
    ) {
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isTrue()
        assertThat(slotPutSharedPreferencesValue.isCaptured).isTrue()

        assertThat(slotPutStringSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotPutSharedPreferencesValue.captured).isNotNull

        val deserializedResult = HTTPResultWithETag.deserialize(slotPutSharedPreferencesValue.captured)
        assertThat(deserializedResult.eTag).isEqualTo(eTagInResponse)
        assertThat(deserializedResult.httpResult.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(deserializedResult.httpResult.payload).isEqualTo(responsePayload)
    }
}
