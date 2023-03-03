package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.common.verification.SigningManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class HTTPClientVerificationTest: BaseHTTPClientTest() {

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
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.SUCCESS),
            verificationStatus = HTTPResult.VerificationStatus.SUCCESS
        )

        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
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
            verificationStatus = HTTPResult.VerificationStatus.NOT_VERIFIED,
            payload = "{\"test-key\":\"test-value\"}"
        )

        enqueue(
            endpoint = endpoint,
            expectedResult = expectedResult,
            verificationStatus = HTTPResult.VerificationStatus.NOT_VERIFIED
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.NOT_VERIFIED)
        verify(exactly = 0) {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on informationalClient verifies response with correct parameters when there is success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        val expectedResult = HTTPResult.createResult(
            verificationStatus = HTTPResult.VerificationStatus.SUCCESS,
            payload = "{\"test-key\":\"test-value\"}"
        )
        val responseCode = expectedResult.responseCode

        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                responseCode,
                expectedResult.payload,
                eTagHeader = any(),
                "/v1${endpoint.getPath()}",
                refreshETag = false,
                verificationStatus = HTTPResult.VerificationStatus.SUCCESS
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
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                endpoint.getPath(),
                responseCode,
                "test-signature",
                "test-nonce",
                "{\"test-key\":\"test-value\"}",
                "1234567890",
                "test-etag"
            )
        }
    }

    @Test
    fun `performRequest on informational client does not throw on verification error`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.ERROR),
            verificationStatus = HTTPResult.VerificationStatus.ERROR
        )

        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.ERROR

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test(expected = SignatureVerificationException::class)
    fun `performRequest on enforced client throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.ERROR),
            verificationStatus = HTTPResult.VerificationStatus.ERROR
        )

        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.ERROR

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
    }

    @Test
    fun `performRequest on enforced client does not throw if verification success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.SUCCESS),
            verificationStatus = HTTPResult.VerificationStatus.SUCCESS
        )

        every {
            mockSigningManager.verifyResponse(any(), any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }
}
