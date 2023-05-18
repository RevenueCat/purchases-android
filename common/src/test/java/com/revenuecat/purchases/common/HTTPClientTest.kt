//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import org.robolectric.annotation.Config as AnnotationConfig

@RunWith(AndroidJUnit4::class)
@AnnotationConfig(manifest = AnnotationConfig.NONE)
class HTTPClientTest: BaseHTTPClientTest() {

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        client = createClient()
    }

    @Test
    fun canPerformASimpleGet() {
        enqueue(
            Endpoint.LogIn,
            expectedResult = HTTPResult.createResult()
        )

        client.performRequest(baseURL, Endpoint.LogIn, null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/v1/subscribers/identify")
    }

    @Test
    fun forwardsTheResponseCode() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult(responseCode = 223)
        )

        val result = client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult(223, "{'response': 'OK'}")
        )

        val result = client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult(payload = "not uh jason")
        )

        try {
            client.performRequest(baseURL, endpoint, null, mapOf("" to ""))
        } finally {
            server.takeRequest()
        }
    }

    // region forceServerErrors

    @Test
    fun `returns server error result when forcing server errors`() {
        val endpoint = Endpoint.LogIn

        client = createClient(appConfig = createAppConfig(forceServerErrors = true))

        val result = client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        assertThat(server.requestCount).isEqualTo(0)
        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.ERROR)
        assertThat(result.payload).isEqualTo("")
        assertThat(result.origin).isEqualTo(HTTPResult.Origin.BACKEND)
        assertThat(result.requestDate).isNull()
        assertThat(result.verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
    }

    @Test
    fun `can dynamically change between getting server errors and not`() {
        val endpoint = Endpoint.LogIn

        val appConfig = createAppConfig(forceServerErrors = true)
        client = createClient(appConfig = appConfig)

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        assertThat(server.requestCount).isEqualTo(0)

        appConfig.forceServerErrors = false

        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult(payload = "{}")
        )

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        assertThat(server.requestCount).isEqualTo(1)
    }

    // endregion forceServerErrors

    // Headers
    @Test
    fun addsHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult
        )

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        client.performRequest(baseURL, endpoint, null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult
        )

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(request.getHeader("X-Platform-Flavor")).isEqualTo(expectedPlatformInfo.flavor)
        assertThat(request.getHeader("X-Platform-Flavor-Version")).isEqualTo(expectedPlatformInfo.version)
        assertThat(request.getHeader("X-Version")).isEqualTo(Config.frameworkVersion)
        assertThat(request.getHeader("X-Client-Locale")).isEqualTo("en-US")
        assertThat(request.getHeader("X-Client-Version")).isEqualTo("")
        assertThat(request.getHeader("X-Client-Bundle-ID")).isEqualTo("mock-package-name")
        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("false")
    }

    @Test
    fun addsETagHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn

        every {
            mockETagManager.getETagHeaders(any(), any())
        } answers {
            mapOf(
                HTTPRequest.ETAG_HEADER_NAME to "mock-etag",
                HTTPRequest.ETAG_LAST_REFRESH_NAME to "1234567890"
            )
        }

        enqueue(
            endpoint,
            expectedResult
        )

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader(HTTPRequest.ETAG_HEADER_NAME)).isEqualTo("mock-etag")
        assertThat(request.getHeader(HTTPRequest.ETAG_LAST_REFRESH_NAME)).isEqualTo("1234567890")
    }

    @Test
    fun doesNotAddNullETagHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn

        every {
            mockETagManager.getETagHeaders(any(), any())
        } answers {
            mapOf(
                HTTPRequest.ETAG_HEADER_NAME to "mock-etag",
                HTTPRequest.ETAG_LAST_REFRESH_NAME to null
            )
        }

        enqueue(
            endpoint,
            expectedResult
        )

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader(HTTPRequest.ETAG_HEADER_NAME)).isEqualTo("mock-etag")
        assertThat(request.headers.names().contains(HTTPRequest.ETAG_LAST_REFRESH_NAME)).isFalse
    }

    @Test
    fun `Given there is no flavor version, flavor version header is not set`() {
        val appConfig = createAppConfig(platformInfo = PlatformInfo("native", null))

        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult
        )

        client = createClient(appConfig)
        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult
        )

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        client.performRequest(baseURL, endpoint, body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
    }

    @Test
    fun `given observer mode is enabled, observer mode header is sent`() {
        val appConfig = createAppConfig()
        appConfig.finishTransactions = false

        client = createClient(appConfig = appConfig)

        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint,
            expectedResult
        )

        client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("true")
    }

    @Test
    fun `clearing caches clears etags`() {
        every {
            mockETagManager.clearCaches()
        } just Runs

        client.clearCaches()

        verify {
            mockETagManager.clearCaches()
        }
    }

    @Test
    fun `if there's an error getting ETag, retry call refreshing ETags`() {
        val response =
            MockResponse()
                .setHeader(HTTPResult.ETAG_HEADER_NAME, "anetag")
                .setResponseCode(RCHTTPStatusCodes.NOT_MODIFIED)

        val expectedResult = HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse)
        val secondResponse =
            MockResponse()
                .setHeader(HTTPResult.ETAG_HEADER_NAME, "anotheretag")
                .setResponseCode(expectedResult.responseCode)
                .setBody(expectedResult.payload)

        server.enqueue(response)
        server.enqueue(secondResponse)

        val endpoint = Endpoint.LogIn
        val urlPathWithVersion = "/v1/subscribers/identify"
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                eTagHeader = any(),
                urlPathWithVersion,
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED
            )
        } returns null

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                payload = expectedResult.payload,
                eTagHeader = any(),
                urlPathWithVersion,
                refreshETag = true,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED
            )
        } returns expectedResult

        val result = client.performRequest(baseURL, endpoint, null, mapOf("" to ""))

        server.takeRequest()
        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeaders(any(), false)
        }
        verify(exactly = 1) {
            mockETagManager.getETagHeaders(any(), true)
        }
        assertThat(result.payload).isEqualTo(expectedResult.payload)
        assertThat(result.responseCode).isEqualTo(expectedResult.responseCode)
    }

    @Test
    fun `performRequest does not add nonce header to request if verification mode disabled`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.VERIFIED)
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("X-Nonce")).isNull()
    }

    @Test
    fun `performRequest uses request time header if present when getting result from etag cache`() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(),
            requestDateHeader = Date(1234567890)
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.SUCCESS,
                "{}",
                eTagHeader = null,
                any(),
                false,
                Date(1234567890),
                VerificationResult.NOT_REQUESTED
            )
        }
    }

    @Test
    fun `payload is returned with trailing new lines`() {
        enqueue(
            Endpoint.LogIn,
            expectedResult = HTTPResult.createResult(payload = "{}\n")
        )

        val result = client.performRequest(baseURL, Endpoint.LogIn, null, emptyMap())

        server.takeRequest()

        assertThat(result.payload).isEqualTo("{}\n")
    }

    // region trackHttpRequestPerformed

    @Test
    fun `performRequest tracks http request performed diagnostic event if request successful`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any()) } just Runs

        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = 200
        val requestStartTime = 1676379370000L // Tuesday, February 14, 2023 12:56:10:000 PM GMT
        val requestEndTime = 1676379370123L // Tuesday, February 14, 2023 12:56:10:123 PM GMT
        val responseTime = (requestEndTime - requestStartTime).milliseconds

        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult()
        )

        every { dateProvider.now } returnsMany listOf(Date(requestStartTime), Date(requestEndTime))

        client.performRequest(baseURL, Endpoint.LogIn, null, mapOf("" to ""))
        server.takeRequest()

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(endpoint, responseTime, true, responseCode, HTTPResult.Origin.BACKEND)
        }
    }

    @Test
    fun `performRequest tracks http request performed diagnostic event if request fails`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any()) } just Runs

        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = RCHTTPStatusCodes.BAD_REQUEST
        val requestStartTime = 1676379370000L // Tuesday, February 14, 2023 12:56:10:000 PM GMT
        val requestEndTime = 1676379370123L // Tuesday, February 14, 2023 12:56:10:123 PM GMT
        val responseTime = (requestEndTime - requestStartTime).milliseconds

        enqueue(
            endpoint,
            expectedResult = HTTPResult.createResult(responseCode = responseCode)
        )

        every { dateProvider.now } returnsMany listOf(Date(requestStartTime), Date(requestEndTime))

        client.performRequest(baseURL, Endpoint.LogIn, null, mapOf("" to ""))
        server.takeRequest()

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(endpoint, responseTime, false, responseCode, HTTPResult.Origin.BACKEND)
        }
    }

    @Test
    fun `performRequest tracks http request performed diagnostic event if request throws Exception`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any()) } just Runs
        every { dateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT
        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = RCHTTPStatusCodes.BAD_REQUEST

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.BAD_REQUEST,
                "not uh json",
                eTagHeader = any(),
                "/v1${endpoint.getPath()}",
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED
            )
        } throws JSONException("bad json")
        val response = MockResponse().setBody("not uh json").setResponseCode(responseCode)
        server.enqueue(response)

        try {
            client.performRequest(baseURL, endpoint, null, mapOf("" to ""))
        } catch (e: JSONException) {
            verify(exactly = 1) {
                diagnosticsTracker.trackHttpRequestPerformed(endpoint, any(), false, HTTPClient.NO_STATUS_CODE, null)
            }
            return
        }
        error("Expected exception")
    }

    // endregion
}
