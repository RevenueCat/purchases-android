package com.revenuecat.purchases.common.networking

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.VerificationResult.*
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.utils.Responses
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
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ETagManagerTest {

    private val mockedPrefs = mockk<SharedPreferences>()
    private val underTest = ETagManager(mockedPrefs)
    private val slotPutStringSharedPreferencesKey = slot<String>()
    private val slotPutSharedPreferencesValue = slot<String>()
    private val mockEditor = mockk<SharedPreferences.Editor>()

    @Before
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
        val eTagHeader = requestWithETagHeader[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isBlank
    }

    @Test
    fun `An ETag header is added if there is an ETag saved for that request`() {
        val path = "/v1/subscribers/appUserID"
        val expectedETag = "etag"
        mockCachedHTTPResult(expectedETag, path)

        val requestWithETagHeader = underTest.getETagHeader(path)
        val eTagHeader = requestWithETagHeader[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `If response code is 304, cached version should be used`() {
        val shouldUse = underTest.shouldUseCachedVersion(RCHTTPStatusCodes.NOT_MODIFIED)

        assertThat(shouldUse).isTrue
    }

    @Test
    fun `If response code is not 304, cached version should not be used`() {
        val shouldUse = underTest.shouldUseCachedVersion(RCHTTPStatusCodes.SUCCESS)

        assertThat(shouldUse).isFalse
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

        val resultFromBackend = HTTPResult.createResult(RCHTTPStatusCodes.NOT_MODIFIED, "")

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
    }

    @Test
    fun `If response code is not 304, store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse
        )
        val resultStored = resultFromBackend.copy(
            origin = HTTPResult.Origin.CACHE
        )
        val resultStoredWithETag = HTTPResultWithETag(eTag, resultStored)

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isTrue
        assertThat(slotPutSharedPreferencesValue.isCaptured).isTrue

        assertThat(slotPutStringSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotPutSharedPreferencesValue.captured).isEqualTo(resultStoredWithETag.serialize())
    }

    @Test
    fun `If response code is 500, don't store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(500)

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
    }

    @Test
    fun `If verification failed, don't store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            verificationResult = FAILED,
            payload = Responses.validEmptyPurchaserResponse
        )

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
    }

    @Test
    fun `If verification successful, store response in cache`() {
        val path = "/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            verificationResult = VERIFIED,
            payload = Responses.validEmptyPurchaserResponse
        )
        val resultStored = resultFromBackend.copy(
            origin = HTTPResult.Origin.CACHE
        )
        val resultStoredWithETag = HTTPResultWithETag(eTag, resultStored)

        underTest.storeBackendResultIfNoError(path, resultFromBackend, eTag)

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isTrue
        assertThat(slotPutSharedPreferencesValue.isCaptured).isTrue

        assertThat(slotPutStringSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotPutSharedPreferencesValue.captured).isEqualTo(resultStoredWithETag.serialize())
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
        val eTagHeader = requestWithETagHeader[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isBlank
    }

    @Test
    fun `if backend returns a 200, store result from backend`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = "{}"
        val eTagInResponse = "etagInResponse"

        underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED
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
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED
        )

        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
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
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result).isNull()
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
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
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = true,
            requestDate = null,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_MODIFIED)
        assertThat(result.payload).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isFalse
        assertThat(slotPutSharedPreferencesValue.isCaptured).isFalse
    }

    @Test
    fun `if it should not use the cached version, use and cache backend result`() {
        val path = "/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payload).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)

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
            eTagHeader = eTagInResponse,
            urlPathWithVersion = path,
            refreshETag = true,
            requestDate = null,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payload).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)

        assertStoredResponse(path, eTagInResponse, responsePayload)
    }

    @Test
    fun `getHTTPResultFromCacheOrBackend should use verification result parameter when coming from backend`() {
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "",
            eTagHeader = "etag",
            urlPathWithVersion = "/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = null,
            verificationResult = VERIFIED
        )

        assertThat(result?.verificationResult).isEqualTo(VERIFIED)
    }

    @Test
    fun `getHTTPResultFromCacheOrBackend should use requestDate from header when no cached version exists`() {
        val expectedDate = Date(1234567890)
        mockCachedHTTPResult(expectedETag = null, "/v1/subscribers/appUserID")
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "",
            eTagHeader = "etag",
            urlPathWithVersion = "/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = expectedDate,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result?.requestDate).isEqualTo(expectedDate)
    }

    @Test
    fun `getHTTPResultFromCacheOrBackend should use requestDate from header even if cached is different`() {
        val expectedDate = Date(1234567890)
        val cachedHttpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.CACHE,
            requestDate = Date(1000)
        )
        mockCachedHTTPResult("etag", "/v1/subscribers/appUserID", cachedHttpResult)
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "",
            eTagHeader = "etag",
            urlPathWithVersion = "/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = expectedDate,
            verificationResult = NOT_REQUESTED
        )

        assertThat(result?.requestDate).isEqualTo(expectedDate)
    }

    @Test
    fun `verificationResults are expected between cache and backend`() {
        data class TestCase(
            val cachedVerificationResult: VerificationResult,
            val backendVerificationResult: VerificationResult,
            val expectedVerificationResult: VerificationResult
            )
        val testCases = listOf(
            TestCase(NOT_REQUESTED, NOT_REQUESTED, NOT_REQUESTED),
            TestCase(NOT_REQUESTED, VERIFIED, VERIFIED),
            TestCase(NOT_REQUESTED, FAILED, FAILED),
            TestCase(VERIFIED, NOT_REQUESTED, NOT_REQUESTED),
            TestCase(VERIFIED, VERIFIED, VERIFIED),
            TestCase(VERIFIED, FAILED, FAILED),
            TestCase(FAILED, NOT_REQUESTED, NOT_REQUESTED),
            TestCase(FAILED, VERIFIED, VERIFIED),
            TestCase(FAILED, FAILED, FAILED)
            )
        testCases.onEach {
            assertCorrectVerificationResult(
                it.cachedVerificationResult, it.backendVerificationResult, it.expectedVerificationResult
            )
        }
    }

    private fun assertCorrectVerificationResult(
        cachedVerificationResult: VerificationResult,
        backendVerificationResult: VerificationResult,
        expectedVerificationResult: VerificationResult
    ) {
        val httpResult = HTTPResult.createResult(
            origin = HTTPResult.Origin.CACHE,
            verificationResult = cachedVerificationResult
        )
        mockCachedHTTPResult("etag", "/v1/subscribers/appUserID", httpResult)
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = "",
            eTagHeader = "etag",
            urlPathWithVersion = "/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = null,
            verificationResult = backendVerificationResult
        )

        assertThat(result?.verificationResult).isEqualTo(expectedVerificationResult)
    }

    private fun mockCachedHTTPResult(
        expectedETag: String?,
        path: String,
        httpResult: HTTPResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE)
    ): HTTPResultWithETag? {
        val cachedResult = expectedETag?.let {
            HTTPResultWithETag(expectedETag, httpResult)
        }
        every {
            mockedPrefs.getString(path, null)
        } returns cachedResult?.serialize()
        return cachedResult
    }

    private fun assertStoredResponse(
        path: String,
        eTagInResponse: String,
        responsePayload: String
    ) {
        assertThat(slotPutStringSharedPreferencesKey.isCaptured).isTrue
        assertThat(slotPutSharedPreferencesValue.isCaptured).isTrue

        assertThat(slotPutStringSharedPreferencesKey.captured).isEqualTo(path)
        assertThat(slotPutSharedPreferencesValue.captured).isNotNull

        val deserializedResult = HTTPResultWithETag.deserialize(slotPutSharedPreferencesValue.captured)
        assertThat(deserializedResult.eTag).isEqualTo(eTagInResponse)
        assertThat(deserializedResult.httpResult.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(deserializedResult.httpResult.payload).isEqualTo(responsePayload)
    }
}
