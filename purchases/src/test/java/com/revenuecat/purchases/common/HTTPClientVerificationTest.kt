package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCContainerTestData
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SignatureVerificationResult
import com.revenuecat.purchases.common.verification.SignatureVerificationResult.FailureReason
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class HTTPClientVerificationTest: BaseHTTPClientTest() {

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Informational>()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns true
        every { mockSigningManager.createRandomNonce() } returns "test-nonce"
        client = createClient()
    }

    @Test
    fun `performRequest adds nonce header to request if endpoint supports it`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Verified),
            verificationResult = SignatureVerificationResult.Verified
        )

        mockSigningResult(SignatureVerificationResult.Verified)

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("X-Nonce")).isEqualTo("test-nonce")
    }

    @Test
    fun `performRequest does not verify response on unsupported endpoints`() {
        val endpoint = Endpoint.PostDiagnostics
        every { mockSigningManager.shouldVerifyEndpoint(endpoint) } returns false
        val expectedResult = HTTPResult.createResult(
            verificationResult = SignatureVerificationResult.NotRequested,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = expectedResult,
            verificationResult = SignatureVerificationResult.NotRequested
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.NotRequested)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest does not verify response on 400 errors`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        every { mockSigningManager.shouldVerifyEndpoint(endpoint) } returns true
        val expectedResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.BAD_REQUEST,
            verificationResult = SignatureVerificationResult.NotRequested,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = expectedResult,
            verificationResult = SignatureVerificationResult.NotRequested
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.NotRequested)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest does not verify response on 500 errors`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        every { mockSigningManager.shouldVerifyEndpoint(endpoint) } returns true
        val expectedResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            verificationResult = SignatureVerificationResult.NotRequested,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = expectedResult,
            verificationResult = SignatureVerificationResult.NotRequested
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.NotRequested)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest on informationalClient verifies response with correct parameters when there is success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        val expectedResult = HTTPResult.createResult(
            verificationResult = SignatureVerificationResult.Verified,
            payload = "{\"test-key\":\"test-value\"}"
        )
        val responseCode = expectedResult.responseCode

        mockSigningResult(SignatureVerificationResult.Verified)

        val urlString = server.url(endpoint.getPath()).toString()
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                responseCode,
                expectedResult.payloadText,
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = false,
                requestDate = Date(1234567890L),
                verificationResult = SignatureVerificationResult.Verified,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns expectedResult
        val response = MockResponse()
            .setBody(expectedResult.payloadText)
            .setResponseCode(responseCode)
            .setHeader(HTTPResult.SIGNATURE_HEADER_NAME, "test-signature")
            .setHeader(HTTPResult.REQUEST_TIME_HEADER_NAME, 1234567890L)
            .setHeader(HTTPResult.ETAG_HEADER_NAME, "test-etag")
        server.enqueue(response)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Verified)
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.contentEquals("{\"test-key\":\"test-value\"}".toByteArray()) },
                "1234567890",
                "test-etag",
                postFieldsToSignHeader = null
            )
        }
    }

    @Test
    fun `performRequest adds post params hash header if verification informational`() {
        val expectedResult = HTTPResult.createResult()
        val expectedPostParamsHash = "test-post-params-hash"
        val endpoint = Endpoint.LogIn
        every {
            mockSigningManager.getPostParamsForSigningHeaderIfNeeded(endpoint, any())
        } returns expectedPostParamsHash
        mockSigningResult(SignatureVerificationResult.Verified)
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult,
            SignatureVerificationResult.Verified,
        )

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"
        body["new_user_id"] = "john"
        val postFieldsToSign = listOf(("user_id" to "jerry"), ("new_user_id" to "john"))

        client.performRequest(baseURL, endpoint, body, postFieldsToSign = postFieldsToSign, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.getHeader(HTTPRequest.POST_PARAMS_HASH)).isNotNull
        assertThat(request.getHeader(HTTPRequest.POST_PARAMS_HASH)).isEqualTo(expectedPostParamsHash)
    }

    @Test
    fun `performRequest does not add post params hash header if verification disabled`() {
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult,
        )

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"
        body["new_user_id"] = "john"
        val postFieldsToSign = listOf(("user_id" to "jerry"), ("new_user_id" to "john"))

        client.performRequest(baseURL, endpoint, body, postFieldsToSign = postFieldsToSign, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.getHeader(HTTPRequest.POST_PARAMS_HASH)).isNull()
    }

    @Test
    fun `performRequest on disabled client does not verify`() {
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.NotRequested),
            verificationResult = SignatureVerificationResult.NotRequested
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.NotRequested)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest on informational client does not throw on verification error`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)),
            verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
        )

        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))
    }

    @Test
    fun `performRequest on informational client without nonce does not throw verification error`() {
        val endpoint = Endpoint.GetOfferings("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)),
            verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
        )

        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))
    }

    @Test
    fun `performRequest passes a null request time to signing when the header is not numeric`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        val expectedResult = HTTPResult.createResult(
            verificationResult = SignatureVerificationResult.Failed(FailureReason.MISSING_REQUEST_TIME),
        )
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                expectedResult.payloadText,
                eTagHeader = any(),
                urlString = server.url(endpoint.getPath()).toString(),
                refreshETag = false,
                requestDate = null,
                verificationResult = expectedResult.verificationResult,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns expectedResult
        server.enqueue(
            MockResponse()
                .setBody(expectedResult.payloadText)
                .setResponseCode(expectedResult.responseCode)
                .setHeader(HTTPResult.SIGNATURE_HEADER_NAME, "test-signature")
                .setHeader(HTTPResult.REQUEST_TIME_HEADER_NAME, "not-a-number"),
        )
        mockSigningResult(expectedResult.verificationResult)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        assertThat(result.requestDate).isNull()
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                urlPath = any(),
                signatureString = "test-signature",
                nonce = any(),
                bodyBytes = any(),
                requestTime = null,
                eTag = any(),
                postFieldsToSignHeader = any(),
            )
        }
    }

    @Test
    fun `performRequest on enforced client throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)),
            verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
        )

        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))

        var thrownCorrectException = false
        try {
            client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                requestHeaders = emptyMap()
            )
        } catch (_: SignatureVerificationException) {
            thrownCorrectException = true
        }

        assertThat(thrownCorrectException).isTrue
        verify(exactly = 0) {
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client keeps verification context in diagnostics`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every {
            diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } just Runs
        val deviceNow = Date(1676379370000L) // Tuesday, February 14, 2023 12:56:10 PM GMT
        val requestDate = Date(deviceNow.time - 2.hours.inWholeMilliseconds)
        client = createClient(
            diagnosticsTracker = diagnosticsTracker,
            dateProvider = object : DateProvider {
                override val now: Date get() = deviceNow
            },
        )
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(),
            requestDateHeader = requestDate,
        )
        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))

        assertThatExceptionOfType(SignatureVerificationException::class.java).isThrownBy {
            client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                requestHeaders = emptyMap()
            )
        }

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                server.hostName,
                endpoint,
                responseTime = any(),
                wasSuccessful = false,
                HTTPClient.NO_STATUS_CODE,
                backendErrorCode = null,
                resultOrigin = null,
                SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH),
                deviceClockOffset = 2.hours,
                isRetry = false,
                connectionErrorReason = null,
            )
        }
    }

    @Test
    fun `performRequest on enforced client in request without nonce throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetOfferings("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)),
            verificationResult = SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
        )

        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))

        assertThatExceptionOfType(SignatureVerificationException::class.java).isThrownBy {
            client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                requestHeaders = emptyMap()
            )
        }

        verify(exactly = 0) {
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client does not throw if verification success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = SignatureVerificationResult.Verified),
            verificationResult = SignatureVerificationResult.Verified
        )

        mockSigningResult(SignatureVerificationResult.Verified)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Verified)
    }

    // region RC Container Format verification

    @Test
    fun `performRequest verifies an RC Container Format response over the container bytes`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val configBytes = "{\"config\":true}".toByteArray()
        val container = RCContainerTestData.buildContainer(config = configBytes)

        mockRCFormatSigningResult(SignatureVerificationResult.Verified)
        enqueueRCFormat(container)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Verified)
        assertThat(result.payload).isInstanceOf(HTTPResult.Payload.RCFormat::class.java)
        // The endpoint requires a nonce, so it is sent on the request and covered by the signature.
        assertThat(recordedRequest.getHeader("X-Nonce")).isEqualTo("test-nonce")
        // The container is handed over as received; SigningManager extracts the signed config part bytes.
        verify(exactly = 1) {
            mockSigningManager.verifyRCFormatResponse(
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.contentEquals(container) },
                "1234567890",
                "test-etag",
            )
        }
    }

    @Test
    fun `performRequest on enforced client throws when RC Format verification fails`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetRemoteConfig("app")
        val container = RCContainerTestData.buildContainer(config = "{\"config\":true}".toByteArray())

        mockRCFormatSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))
        enqueueRCFormat(container)

        assertThatExceptionOfType(SignatureVerificationException::class.java).isThrownBy {
            client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                requestHeaders = emptyMap()
            )
        }
    }

    @Test
    fun `performRequest verifies a 204 empty response over the request context with an empty body`() {
        val endpoint = Endpoint.GetRemoteConfig("app")

        mockSigningResult(SignatureVerificationResult.Verified)
        enqueueRCFormat(ByteArray(0), responseCode = RCHTTPStatusCodes.NO_CONTENT)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.NO_CONTENT)
        assertThat(result.verificationResult).isEqualTo(SignatureVerificationResult.Verified)
        // A 204 has no body, so the signed payload is the request context plus an empty body.
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.isEmpty() },
                "1234567890",
                "test-etag",
                postFieldsToSignHeader = null
            )
        }
    }

    @Test
    fun `performRequest on enforced client throws when a 204 fails verification`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetRemoteConfig("app")

        mockSigningResult(SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH))
        enqueueRCFormat(ByteArray(0), responseCode = RCHTTPStatusCodes.NO_CONTENT)

        assertThatExceptionOfType(SignatureVerificationException::class.java).isThrownBy {
            client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                requestHeaders = emptyMap()
            )
        }
    }

    // endregion

    private fun mockSigningResult(result: SignatureVerificationResult) {
        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns result
    }

    private fun mockRCFormatSigningResult(result: SignatureVerificationResult) {
        every {
            mockSigningManager.verifyRCFormatResponse(any(), any(), any(), any(), any(), any())
        } returns result
    }

    private fun assertSigningNotPerformed() {
        verify(exactly = 0) {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            mockSigningManager.verifyRCFormatResponse(any(), any(), any(), any(), any(), any())
        }
    }

    private fun enqueueRCFormat(body: ByteArray, responseCode: Int = RCHTTPStatusCodes.SUCCESS) {
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(body))
                .setResponseCode(responseCode)
                .setHeader(HTTPResult.SIGNATURE_HEADER_NAME, "test-signature")
                .setHeader(HTTPResult.REQUEST_TIME_HEADER_NAME, 1234567890L)
                .setHeader(HTTPResult.ETAG_HEADER_NAME, "test-etag")
        )
    }
}
