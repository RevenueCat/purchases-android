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
    fun `ETag header is empty added if there is no ETag saved for that request`() {
        val path = "v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, path = path)

        val requestWithETagHeader = underTest.getETagHeader(path)
        val eTagHeader = requestWithETagHeader[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isBlank()
    }

    @Test
    fun `An ETag header is added if there is an ETag saved for that request`() {
        val path = "v1/subscribers/appUserID"
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
        val path = "v1/subscribers/appUserID"
        val eTag = "eTag"

        val cachedHTTPResult = mockCachedHTTPResult(eTag, path)!!.httpResult

        val storedResult = underTest.getStoredResult(path)

        assertThat(storedResult).isNotNull
        assertThat(storedResult!!.payload).isEqualTo(cachedHTTPResult.payload)
        assertThat(storedResult.responseCode).isEqualTo(cachedHTTPResult.responseCode)
    }

    @Test
    fun `GetStoredResult returns null if there's nothing cached`() {
        val path = "v1/subscribers/appUserID"
        every {
            mockedPrefs.getString(path, null)
        } returns null
        val storedResult = underTest.getStoredResult(path)

        assertThat(storedResult).isNull()
    }

    @Test
    fun `If response code is 304, don't store response in cache`() {
        val path = "v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(RCHTTPStatusCodes.NOT_MODIFIED, "")

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotSharedPreferencesValue.isCaptured).isFalse()
    }

    @Test
    fun `If response code is not 304, store response in cache`() {
        val path = "v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse)
        val resultFromBackendWithETag = HTTPResultWithETag(eTag, resultFromBackend)

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotSharedPreferencesKey.isCaptured).isTrue()
        assertThat(slotSharedPreferencesValue.isCaptured).isTrue()

        assertThat(slotSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotSharedPreferencesValue.captured).isEqualTo(resultFromBackendWithETag.serialize())
    }

    @Test
    fun `If response code is 500, don't store response in cache`() {
        val path = "v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult(500, "{}")

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotSharedPreferencesKey.isCaptured).isFalse()
        assertThat(slotSharedPreferencesValue.isCaptured).isFalse()
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
        val path = "v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, path = path)

        val requestWithETagHeader = underTest.getETagHeader(path, refreshETag = true)
        val eTagHeader = requestWithETagHeader[ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull()
        assertThat(eTagHeader).isBlank()
    }

    private fun mockURLConnectionETag(eTag: String): HttpURLConnection {
        val mockedConnection = mockk<HttpURLConnection>(relaxed = true).also {
            every { it.getHeaderField(ETAG_HEADER_NAME) } returns eTag
        }
        return mockedConnection
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
}