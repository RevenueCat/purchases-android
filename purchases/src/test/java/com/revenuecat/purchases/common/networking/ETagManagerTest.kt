package com.revenuecat.purchases.common.networking

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.VerificationResult.*
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ETagManagerTest {

    private val testDate = Date(1675954145L) // Thursday, February 9, 2023 2:49:05 PM GMT
    private val testDateProvider = object : DateProvider {
        override val now: Date
            get() = testDate
    }
    private val mockedPrefs = mockk<SharedPreferences>()
    private val payloadStore: ETagPayloadStore by lazy { spyk(ETagPayloadStore(temporaryFolder.newFolder())) }
    private val underTest: ETagManager by lazy {
        ETagManager(mockk(), lazy { mockedPrefs }, testDateProvider, payloadStore)
    }
    private val putStringKeys = mutableListOf<String>()
    private val putStringValues = mutableListOf<String>()
    private val mockEditor = mockk<SharedPreferences.Editor>()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        every {
            mockedPrefs.edit()
        } returns mockEditor
        every {
            mockedPrefs.getString(any(), null)
        } returns null
        every {
            mockEditor.putString(capture(putStringKeys), capture(putStringValues))
        } returns mockEditor
        every {
            mockEditor.apply()
        } just Runs
        every {
            mockEditor.commit()
        } returns true
        every {
            mockEditor.clear()
        } returns mockEditor
    }

    @Test
    fun `ETag header is empty added if there is no ETag saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isBlank
    }

    @Test
    fun `ETag Last refresh time header is null if there is no ETag saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val lastRefreshTimeHeader = eTagHeaders[HTTPRequest.ETAG_LAST_REFRESH_NAME]
        assertThat(lastRefreshTimeHeader).isNull()
    }

    @Test
    fun `ETag Last refresh time header is null if there is an ETag saved but no refresh time saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = "etag", expectedLastRefreshTime = null, urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo("etag")

        val lastRefreshTimeHeader = eTagHeaders[HTTPRequest.ETAG_LAST_REFRESH_NAME]
        assertThat(lastRefreshTimeHeader).isNull()
    }

    @Test
    fun `An ETag header is added if there is an ETag saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val expectedETag = "etag"
        mockCachedHTTPResult(expectedETag, urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `An ETag Last refresh time header is added if there is a refresh time saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = "etag", expectedLastRefreshTime = testDate, urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val lastRefreshTimeHeader = eTagHeaders[HTTPRequest.ETAG_LAST_REFRESH_NAME]
        assertThat(lastRefreshTimeHeader).isEqualTo("1675954145")
    }

    @Test
    fun `Expected number of headers are added when there is an Etag and last refresh time saved for that request`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = "etag", expectedLastRefreshTime = testDate, urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        assertThat(eTagHeaders.size).isEqualTo(2)
    }

    // region ETag headers usage verification tests

    @Test
    fun `ETag headers are added if cached result is verified and verification not requested`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val expectedETag = "etag"
        mockCachedHTTPResult(
            expectedETag,
            urlString,
            httpResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE, verificationResult = VERIFIED),
        )

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `ETag headers are added if cached result is not requested and verification not requested`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val expectedETag = "etag"
        mockCachedHTTPResult(
            expectedETag,
            urlString,
            httpResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE, verificationResult = NOT_REQUESTED),
        )

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo(expectedETag)
    }

    @Test
    fun `ETag headers are not added if cached result is not requested and verification requested`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(
            "etag",
            urlString,
            httpResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE, verificationResult = NOT_REQUESTED),
        )

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = true)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo("")
    }

    @Test
    fun `ETag headers are not added if cached result errored`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(
            "etag",
            urlString,
            httpResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE, verificationResult = FAILED),
        )

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo("")
    }

    @Test
    fun `ETag headers are not added if cached result verified on device`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(
            "etag",
            urlString,
            httpResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE, verificationResult = VERIFIED_ON_DEVICE),
        )

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)
        val eTagHeader = eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isEqualTo("")
    }

    // endregion ETag headers usage verification tests

    @Test
    fun `getETagHeaders does not read the cached payload`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = "etag", urlString = urlString)

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)

        assertThat(eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]).isEqualTo("etag")
        verify(exactly = 0) { payloadStore.read(any()) }
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
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"
        val cachedHTTPResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE)

        mockCachedHTTPResult(eTag, urlString, httpResult = cachedHTTPResult)

        val storedResult = underTest.getStoredResult(urlString)

        assertThat(storedResult).isNotNull
        assertThat(storedResult!!.payload).isEqualTo(cachedHTTPResult.payload)
        assertThat(storedResult.responseCode).isEqualTo(cachedHTTPResult.responseCode)
    }

    @Test
    fun `GetStoredResult returns null if there's nothing cached`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        every {
            mockedPrefs.getString(urlString, null)
        } returns null
        val storedResult = underTest.getStoredResult(urlString)

        assertThat(storedResult).isNull()
    }

    @Test
    fun `If response code is 304, don't store response in cache`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(RCHTTPStatusCodes.NOT_MODIFIED, "")

        underTest.storeBackendResultIfNoError(urlString, resultFromBackend, eTag)

        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `If response code is not 304, store response in cache`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse
        )

        underTest.storeBackendResultIfNoError(urlString, resultFromBackend, eTag)

        assertStoredResponse(urlString, eTag, testDate, Responses.validEmptyPurchaserResponse)
    }

    @Test
    fun `If response code is 500, don't store response in cache`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(500)

        underTest.storeBackendResultIfNoError(urlString, resultFromBackend, eTag)

        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `If verification failed, don't store response in cache`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            verificationResult = FAILED,
            payload = Responses.validEmptyPurchaserResponse
        )

        underTest.storeBackendResultIfNoError(urlString, resultFromBackend, eTag)

        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `If verification successful, store response in cache`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val eTag = "eTag"

        val resultFromBackend = HTTPResult.createResult(
            verificationResult = VERIFIED,
            payload = Responses.validEmptyPurchaserResponse
        )

        underTest.storeBackendResultIfNoError(urlString, resultFromBackend, eTag)

        assertStoredResponse(urlString, eTag, testDate, Responses.validEmptyPurchaserResponse)
    }

    @Test
    fun `storeBackendResultIfNoError stores metadata in prefs and the payload in the payload store`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val payload = "{\"key\":\"value\"}"
        val result = HTTPResult.createResult(payload = payload)

        underTest.storeBackendResultIfNoError(urlString, result, eTagInResponse = "etag")

        assertStoredResponse(urlString, "etag", testDate, payload)
    }

    @Test
    fun `storeBackendResultIfNoError stores the payload without JSON re-encoding`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        // Quote-dense payload: the legacy combined format would escape every quote twice.
        val payload = "{\"offerings\":[{\"id\":\"premium\",\"desc\":\"a \\\"quoted\\\" name\"}]}"
        val result = HTTPResult.createResult(payload = payload)

        underTest.storeBackendResultIfNoError(urlString, result, eTagInResponse = "etag")

        // Byte-for-byte what we received: never escaped, and never written into the prefs JSON.
        // (ETagManagerMemoryTest gates the allocation cost of this path.)
        assertThat(payloadStore.read(urlString)).isEqualTo(payload)
        assertThat(putStringKeys).containsExactly(ETagManager.metadataKey(urlString))
    }

    @Test
    fun `storeBackendResultIfNoError does not mutate the result being stored`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val result = HTTPResult.createResult(origin = HTTPResult.Origin.BACKEND)

        underTest.storeBackendResultIfNoError(urlString, result, eTagInResponse = "etag")

        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)
    }

    @Test
    fun `Clearing caches removes all shared preferences and stored payloads`() {
        payloadStore.write("http://localhost:100/v1/subscribers/appUserID", "payload")

        underTest.clearCaches()

        assertThat(payloadStore.read("http://localhost:100/v1/subscribers/appUserID")).isNull()
        verify {
            mockEditor.clear()
        }
    }

    @Test
    fun `HTTP Header is empty when refreshing etag`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult(expectedETag = null, urlString = urlString)

        val requestWithETagHeader = underTest.getETagHeaders(urlString, verificationRequested = false, refreshETag = true)
        val eTagHeader = requestWithETagHeader[HTTPRequest.ETAG_HEADER_NAME]
        assertThat(eTagHeader).isNotNull
        assertThat(eTagHeader).isBlank
    }

    @Test
    fun `if backend returns a 200, store result from backend`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = "{}"
        val eTagInResponse = "etagInResponse"

        underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertStoredResponse(urlString, eTagInResponse, testDate, responsePayload)
    }

    @Test
    fun `don't store result from backend if the response code is 304 and there is a cached result`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val expectedETag = "etag"
        mockCachedHTTPResult(expectedETag, urlString)

        underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `if should use cached version, but there's nothing cached, return null when getting result and don't cache anything`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        every {
            mockedPrefs.getString(urlString, null)
        } returns null

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result).isNull()
        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `if should use cached version, but there's nothing cached, use backend result and don't cache anything if etag is already being refreshed`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        every {
            mockedPrefs.getString(urlString, null)
        } returns null

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = true,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_MODIFIED)
        assertThat(result.payloadText).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)
        assertThat(putStringKeys).isEmpty()
    }

    @Test
    fun `if it should not use the cached version, use and cache backend result`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = false,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payloadText).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)

        assertStoredResponse(urlString, eTagInResponse, testDate, responsePayload)
    }

    @Test
    fun `if should not use cached version, and it's refreshing the etag, use and cache backend result`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val responsePayload = ""
        val eTagInResponse = "etagInResponse"

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = responsePayload,
            eTagHeader = eTagInResponse,
            urlString = urlString,
            refreshETag = true,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result).isNotNull
        assertThat(result!!.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payloadText).isEqualTo(responsePayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)

        assertStoredResponse(urlString, eTagInResponse, testDate, responsePayload)
    }

    @Test
    fun `getHTTPResultFromCacheOrBackend should use verification result parameter when coming from backend`() {
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "",
            eTagHeader = "etag",
            urlString = "http://localhost:100/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = null,
            verificationResult = VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
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
            urlString = "http://localhost:100/v1/subscribers/appUserID",
            refreshETag = false,
            requestDate = expectedDate,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
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
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult("etag", urlString, httpResult = cachedHttpResult)
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = "",
            eTagHeader = "etag",
            urlString = urlString,
            refreshETag = false,
            requestDate = expectedDate,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result?.requestDate).isEqualTo(expectedDate)
    }

    @Test
    fun `getHTTPResultFromCacheOrBackend on 304 returns cached payload and origin while updating verification and requestDate`() {
        val cachedPayload = "{\"cached\":true}"
        val newRequestDate = Date(1234567890)
        val cachedHttpResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.SUCCESS,
            payload = cachedPayload,
            origin = HTTPResult.Origin.CACHE,
            requestDate = Date(1000),
            verificationResult = NOT_REQUESTED,
        )
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult("etag", urlString, httpResult = cachedHttpResult)

        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = "",
            eTagHeader = "etag",
            urlString = urlString,
            refreshETag = false,
            requestDate = newRequestDate,
            verificationResult = VERIFIED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result).isNotNull
        assertThat(result!!.payloadText).isEqualTo(cachedPayload)
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.CACHE)
        assertThat(result.verificationResult).isEqualTo(VERIFIED)
        assertThat(result.requestDate).isEqualTo(newRequestDate)
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

    @Test
    fun `ETagCacheMetadata serialization round-trips all fields`() {
        val metadata = ETagCacheMetadata(
            eTagData = ETagData("etag", testDate),
            responseCode = RCHTTPStatusCodes.SUCCESS,
            requestDate = Date(1234567890),
            verificationResult = VERIFIED,
            isLoadShedderResponse = true,
            isFallbackURL = true,
            payloadSizeBytes = 12345L,
        )

        val deserialized = ETagCacheMetadata.deserialize(metadata.serialize())

        assertThat(deserialized).isEqualTo(metadata)
    }

    @Test
    fun `ETagCacheMetadata serialization round-trips null dates`() {
        val metadata = ETagCacheMetadata(
            eTagData = ETagData("etag", lastRefreshTime = null),
            responseCode = RCHTTPStatusCodes.SUCCESS,
            requestDate = null,
            verificationResult = NOT_REQUESTED,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        val deserialized = ETagCacheMetadata.deserialize(metadata.serialize())

        assertThat(deserialized).isEqualTo(metadata)
    }

    @Test
    fun `a legacy combined entry reads as unparseable metadata`() {
        // Pre-split format: eTag and lastRefreshTime at the top level, everything else nested inside an
        // escaped httpResult string. Must never parse as split-format metadata.
        val legacyEntry = """{"eTag":"etag","lastRefreshTime":1675954145,"httpResult":"{\"responseCode\":200}"}"""

        assertThat(ETagCacheMetadata.deserialize(legacyEntry)).isNull()
    }

    @Test
    fun `ETagCacheMetadata deserialize returns null for unparseable data`() {
        assertThat(ETagCacheMetadata.deserialize("not json")).isNull()
    }

    @Test
    fun `an unparseable cache entry is treated as a miss`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        every { mockedPrefs.getString(urlString, null) } returns "not json"

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)

        assertThat(eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]).isEmpty()
    }

    @Test
    fun `a v2 entry with an unknown verification result reads as a miss`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val metadata = ETagCacheMetadata.fromResult(
            HTTPResult.createResult(origin = HTTPResult.Origin.CACHE),
            ETagData("etag", testDate),
        )
        val corrupted = metadata.serialize().replace("NOT_REQUESTED", "SOMETHING_UNKNOWN")
        every { mockedPrefs.getString(ETagManager.metadataKey(urlString), null) } returns corrupted

        val eTagHeaders = underTest.getETagHeaders(urlString, verificationRequested = false)

        assertThat(eTagHeaders[HTTPRequest.ETAG_HEADER_NAME]).isEmpty()
    }

    @Test
    fun `a payload file whose size does not match the metadata is treated as a miss`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val payload = "{\"key\":\"value\"}"
        val metadata = ETagCacheMetadata.fromResult(
            HTTPResult.createResult(payload = payload, origin = HTTPResult.Origin.CACHE),
            ETagData("etag", testDate),
        ).copy(payloadSizeBytes = payload.length + 100L)
        every { mockedPrefs.getString(ETagManager.metadataKey(urlString), null) } returns metadata.serialize()
        payloadStore.write(urlString, payload)

        assertThat(underTest.getStoredResult(urlString)).isNull()
    }

    @Test
    fun `metadata without a stored payload is treated as a miss on read`() {
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        val metadata = ETagCacheMetadata.fromResult(
            HTTPResult.createResult(origin = HTTPResult.Origin.CACHE),
            ETagData("etag", testDate),
        )
        every { mockedPrefs.getString(ETagManager.metadataKey(urlString), null) } returns metadata.serialize()

        assertThat(underTest.getStoredResult(urlString)).isNull()
    }

    @Test
    fun `ETagCacheMetadata toHTTPResult rebuilds the result with CACHE origin`() {
        val metadata = ETagCacheMetadata.fromResult(
            HTTPResult.createResult(
                responseCode = RCHTTPStatusCodes.SUCCESS,
                payload = "{\"key\":\"value\"}",
                origin = HTTPResult.Origin.BACKEND,
                requestDate = Date(1000),
                verificationResult = VERIFIED,
            ),
            ETagData("etag", testDate),
        )

        val result = metadata.toHTTPResult("{\"key\":\"value\"}")

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(result.payloadText).isEqualTo("{\"key\":\"value\"}")
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.CACHE)
        assertThat(result.requestDate).isEqualTo(Date(1000))
        assertThat(result.verificationResult).isEqualTo(VERIFIED)
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
        val urlString = "http://localhost:100/v1/subscribers/appUserID"
        mockCachedHTTPResult("etag",urlString, httpResult = httpResult)
        val result = underTest.getHTTPResultFromCacheOrBackend(
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            payload = "",
            eTagHeader = "etag",
            urlString = urlString,
            refreshETag = false,
            requestDate = null,
            verificationResult = backendVerificationResult,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        assertThat(result?.verificationResult).isEqualTo(expectedVerificationResult)
    }

    private fun mockCachedHTTPResult(
        expectedETag: String?,
        urlString: String,
        expectedLastRefreshTime: Date? = Date(),
        httpResult: HTTPResult = HTTPResult.createResult(origin = HTTPResult.Origin.CACHE)
    ): ETagCacheMetadata? {
        val metadata = expectedETag?.let {
            ETagCacheMetadata.fromResult(httpResult, ETagData(expectedETag, expectedLastRefreshTime))
        }
        every {
            mockedPrefs.getString(ETagManager.metadataKey(urlString), null)
        } returns metadata?.serialize()
        if (metadata != null) {
            payloadStore.write(urlString, httpResult.payloadText)
        }
        return metadata
    }

    private fun assertStoredResponse(
        urlString: String,
        eTagInResponse: String,
        lastRefreshTime: Date?,
        responsePayload: String
    ) {
        val storedByKey = putStringKeys.zip(putStringValues).toMap()

        val metadata = ETagCacheMetadata.deserialize(storedByKey.getValue(ETagManager.metadataKey(urlString)))
        assertThat(metadata).isNotNull
        assertThat(metadata!!.eTagData.eTag).isEqualTo(eTagInResponse)
        assertThat(metadata.eTagData.lastRefreshTime?.time).isEqualTo(lastRefreshTime?.time)
        assertThat(metadata.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(metadata.payloadSizeBytes).isEqualTo(responsePayload.toByteArray().size.toLong())

        assertThat(payloadStore.read(urlString)).isEqualTo(responsePayload)
    }
}
