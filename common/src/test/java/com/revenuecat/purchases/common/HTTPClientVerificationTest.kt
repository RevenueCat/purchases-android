package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verification.SignatureVerificationException
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

    private lateinit var signingManager: SigningManager

    private lateinit var informationalVerificationClient: HTTPClient
    private lateinit var enforcedVerificationClient: HTTPClient

    @Before
    fun setupClient() {
        signingManager = mockk()
        every { signingManager.createRandomNonce() } returns "test-nonce"
        informationalVerificationClient = createClient(
            signingManagerIfEnabled = signingManager,
            verificationMode = EntitlementVerificationMode.INFORMATIONAL
        )
        enforcedVerificationClient = createClient(
            signingManagerIfEnabled = signingManager,
            verificationMode = EntitlementVerificationMode.ENFORCED
        )
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
            signingManager.verifyResponse(any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        informationalVerificationClient.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("X-Nonce")).isEqualTo("test-nonce")
    }

    @Test
    fun `performRequest on informationalClient verifies response with correct parameters when there is success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        val expectedResult = HTTPResult.createResult(
            verificationStatus = HTTPResult.VerificationStatus.SUCCESS,
            payload = "{\"test-key\":\"test-value\"}"
        )

        every {
            signingManager.verifyResponse(any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                expectedResult.payload,
                eTagHeader = any(),
                "/v1${endpoint.getPath()}",
                refreshETag = false,
                verificationStatus = HTTPResult.VerificationStatus.SUCCESS
            )
        } returns expectedResult
        val response = MockResponse()
            .setBody(expectedResult.payload)
            .setResponseCode(expectedResult.responseCode)
            .setHeader(HTTPResult.SIGNATURE_HEADER_NAME, "test-signature")
            .setHeader(HTTPResult.REQUEST_TIME_HEADER_NAME, 1234567890L)
            .setHeader(HTTPResult.ETAG_HEADER_NAME, "test-etag")
        server.enqueue(response)

        val result = informationalVerificationClient.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
        verify(exactly = 1) {
            signingManager.verifyResponse(
                endpoint.getPath(),
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
            signingManager.verifyResponse(any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.ERROR

        val result = informationalVerificationClient.performRequest(
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
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.ERROR),
            verificationStatus = HTTPResult.VerificationStatus.ERROR
        )

        every {
            signingManager.verifyResponse(any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.ERROR

        enforcedVerificationClient.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
    }

    fun `performRequest on enforced client does not throw if verification success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            endpoint = endpoint,
            expectedResult = HTTPResult.createResult(verificationStatus = HTTPResult.VerificationStatus.SUCCESS),
            verificationStatus = HTTPResult.VerificationStatus.SUCCESS
        )

        every {
            signingManager.verifyResponse(any(), any(), any(), any(), any(), any())
        } returns HTTPResult.VerificationStatus.SUCCESS

        val result = enforcedVerificationClient.performRequest(
            baseURL,
            endpoint,
            body = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()
        assertThat(result.verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }
}
