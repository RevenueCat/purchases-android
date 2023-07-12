package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

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
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.VERIFIED),
            verificationResult = VerificationResult.VERIFIED
        )

        mockSigningResult(VerificationResult.VERIFIED)

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
            verificationResult = VerificationResult.NOT_REQUESTED,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            endpoint = endpoint,
            expectedResult = expectedResult,
            verificationResult = VerificationResult.NOT_REQUESTED
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest does not verify response on 400 errors`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        every { mockSigningManager.shouldVerifyEndpoint(endpoint) } returns true
        val expectedResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.BAD_REQUEST,
            verificationResult = VerificationResult.NOT_REQUESTED,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            endpoint = endpoint,
            expectedResult = expectedResult,
            verificationResult = VerificationResult.NOT_REQUESTED
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest does not verify response on 500 errors`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        every { mockSigningManager.shouldVerifyEndpoint(endpoint) } returns true
        val expectedResult = HTTPResult.createResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            verificationResult = VerificationResult.NOT_REQUESTED,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            endpoint = endpoint,
            expectedResult = expectedResult,
            verificationResult = VerificationResult.NOT_REQUESTED
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest on informationalClient verifies response with correct parameters when there is success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        val expectedResult = HTTPResult.createResult(
            verificationResult = VerificationResult.VERIFIED,
            payload = "{\"test-key\":\"test-value\"}"
        )
        val responseCode = expectedResult.responseCode

        mockSigningResult(VerificationResult.VERIFIED)

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                responseCode,
                expectedResult.payload,
                eTagHeader = any(),
                "/v1${endpoint.getPath()}",
                refreshETag = false,
                requestDate = Date(1234567890L),
                verificationResult = VerificationResult.VERIFIED
            )
        } returns expectedResult
        val response = MockResponse()
            .setBody(expectedResult.payload)
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

        assertThat(result.verificationResult).isEqualTo(VerificationResult.VERIFIED)
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                "/v1${endpoint.getPath()}",
                "test-signature",
                "test-nonce",
                "{\"test-key\":\"test-value\"}",
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
        mockSigningResult(VerificationResult.VERIFIED)
        enqueue(
            endpoint,
            expectedResult,
            VerificationResult.VERIFIED,
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
            endpoint,
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
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.NOT_REQUESTED),
            verificationResult = VerificationResult.NOT_REQUESTED
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest on informational client does not throw on verification error`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.FAILED),
            verificationResult = VerificationResult.FAILED
        )

        mockSigningResult(VerificationResult.FAILED)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `performRequest on informational client without nonce does not throw verification error`() {
        val endpoint = Endpoint.GetOfferings("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.FAILED),
            verificationResult = VerificationResult.FAILED
        )

        mockSigningResult(VerificationResult.FAILED)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `performRequest on enforced client throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.FAILED),
            verificationResult = VerificationResult.FAILED
        )

        mockSigningResult(VerificationResult.FAILED)

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
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client in request without nonce throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetOfferings("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.FAILED),
            verificationResult = VerificationResult.FAILED
        )

        mockSigningResult(VerificationResult.FAILED)

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
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client does not throw if verification success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.VERIFIED),
            verificationResult = VerificationResult.VERIFIED
        )

        mockSigningResult(VerificationResult.VERIFIED)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    private fun mockSigningResult(result: VerificationResult) {
        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns result
    }

    private fun assertSigningNotPerformed() {
        verify(exactly = 0) {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        }
    }
}
