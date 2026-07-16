package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCContentEncoding
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.every
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
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Date
import java.util.zip.GZIPOutputStream

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
            urlPath = endpoint.getPath(),
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
            urlPath = endpoint.getPath(),
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
            urlPath = endpoint.getPath(),
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

        val urlString = server.url(endpoint.getPath()).toString()
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                responseCode,
                expectedResult.payloadText,
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = false,
                requestDate = Date(1234567890L),
                verificationResult = VerificationResult.VERIFIED,
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

        assertThat(result.verificationResult).isEqualTo(VerificationResult.VERIFIED)
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
        mockSigningResult(VerificationResult.VERIFIED)
        enqueue(
            urlPath = endpoint.getPath(),
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
            urlPath = endpoint.getPath(),
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
            urlPath = endpoint.getPath(),
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
            urlPath = endpoint.getPath(),
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
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client in request without nonce throws verification error`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetOfferings("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
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
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `performRequest on enforced client does not throw if verification success`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
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

    // region RC Container Format verification

    @Test
    fun `performRequest verifies an RC Container Format response over the config part bytes`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val configBytes = "{\"config\":true}".toByteArray()
        val container = buildContainer(configBytes)

        mockSigningResult(VerificationResult.VERIFIED)
        enqueueRCFormat(container)

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(VerificationResult.VERIFIED)
        assertThat(result.payload).isInstanceOf(HTTPResult.Payload.RCFormat::class.java)
        // The endpoint requires a nonce, so it is sent on the request and covered by the signature.
        assertThat(recordedRequest.getHeader("X-Nonce")).isEqualTo("test-nonce")
        // The signed payload is the config part bytes exactly as received, not the element checksum.
        verify(exactly = 1) {
            mockSigningManager.verifyResponse(
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.contentEquals(configBytes) },
                "1234567890",
                "test-etag",
                postFieldsToSignHeader = null
            )
        }
    }

    @Test
    fun `performRequest verifies the config part bytes regardless of the element checksum`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val configBytes = "{\"config\":true}".toByteArray()
        // The element checksum is an untrusted lookup hint: a mismatch must not affect verification, which is
        // anchored on the signature over the config part bytes.
        val container = buildContainer(configBytes, configChecksum = ByteArray(24) { 0x7F })

        mockSigningResult(VerificationResult.VERIFIED)
        enqueueRCFormat(container)

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
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.contentEquals(configBytes) },
                "1234567890",
                "test-etag",
                postFieldsToSignHeader = null
            )
        }
    }

    @Test
    fun `performRequest verifies a GZIP-compressed config element over its decoded bytes`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val configBytes = "{\"config\":true}".toByteArray()
        // Once the config element crosses the backend's size threshold it is served GZIP-compressed (codec 1).
        // The signature covers the uncompressed config bytes, so verification must decode before signing.
        val container = buildContainer(configBytes, codec = RCContentEncoding.GZIP.id)

        mockSigningResult(VerificationResult.VERIFIED)
        enqueueRCFormat(container)

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
                urlPath = endpoint.getPath(),
                "test-signature",
                "test-nonce",
                match<ByteArray> { it.contentEquals(configBytes) },
                "1234567890",
                "test-etag",
                postFieldsToSignHeader = null
            )
        }
    }

    @Test
    fun `performRequest fails RC Format verification when the response is not a valid RC Container`() {
        val endpoint = Endpoint.GetRemoteConfig("app")

        mockSigningResult(VerificationResult.VERIFIED)
        enqueueRCFormat(byteArrayOf(1, 2, 3, 4))

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        server.takeRequest()

        assertThat(result.verificationResult).isEqualTo(VerificationResult.FAILED)
        assertSigningNotPerformed()
    }

    @Test
    fun `performRequest on enforced client throws when RC Format verification fails`() {
        every { mockSigningManager.signatureVerificationMode } returns mockk<SignatureVerificationMode.Enforced>()
        val endpoint = Endpoint.GetRemoteConfig("app")
        val container = buildContainer("{\"config\":true}".toByteArray())

        mockSigningResult(VerificationResult.FAILED)
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

        mockSigningResult(VerificationResult.VERIFIED)
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
        assertThat(result.verificationResult).isEqualTo(VerificationResult.VERIFIED)
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

        mockSigningResult(VerificationResult.FAILED)
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

    @Suppress("MagicNumber")
    private fun buildContainer(
        config: ByteArray,
        configChecksum: ByteArray = sha256Truncated(config),
        codec: Int = RCContentEncoding.NONE.id,
    ): ByteArray {
        // The checksum always covers the uncompressed config; only the on-wire body is (optionally) compressed.
        val body = if (codec == RCContentEncoding.GZIP.id) gzip(config) else config
        val out = ByteArrayOutputStream()
        out.write('R'.code)
        out.write('C'.code)
        out.write(1) // version
        out.write(0) // flags
        repeat(4) { out.write(0) } // header reserved
        out.write(configChecksum)
        out.writeUInt32LE(body.size)
        out.writeUInt32LE(codec) // element reserved; low byte is the content-encoding codec id
        out.write(body)
        while (out.size() % 8 != 0) {
            out.write(0)
        }
        return out.toByteArray()
    }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    @Suppress("MagicNumber")
    private fun ByteArrayOutputStream.writeUInt32LE(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    @Suppress("MagicNumber")
    private fun sha256Truncated(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data).copyOf(24)
}
