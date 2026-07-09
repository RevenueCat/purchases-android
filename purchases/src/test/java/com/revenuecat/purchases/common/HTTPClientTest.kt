//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ForceServerErrorStrategy
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.HTTPTimeoutManager
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSource
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceProvider
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import org.robolectric.annotation.Config as AnnotationConfig

@RunWith(AndroidJUnit4::class)
@AnnotationConfig(manifest = AnnotationConfig.NONE)
internal class HTTPClientTest: BaseHTTPClientTest() {

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        client = createClient()
    }

    @Test
    fun canPerformASimpleGet() {
        enqueue(
            Endpoint.LogIn.getPath(),
            expectedResult = HTTPResult.createResult()
        )

        client.performRequest(baseURL, Endpoint.LogIn, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/v1/subscribers/identify")
    }

    // region API source base host

    @Test
    fun `performRequest resolves base host from the API source provider when baseURL is the default host`() {
        val endpoint = Endpoint.GetCustomerInfo("test_user_id")
        val provider = FakeAPISourceProvider(listOf(server.url("/").toString()))
        val client = createClient(
            appConfig = createAppConfig(proxyURL = null),
            apiSourceProvider = provider,
        )
        enqueue(endpoint.getPath(), expectedResult = HTTPResult.createResult())

        client.performRequest(
            URL(AppConfig.baseUrlString),
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `performRequest uses the provided base URL when there is no API source provider`() {
        val endpoint = Endpoint.GetCustomerInfo("test_user_id")
        val client = createClient(
            appConfig = createAppConfig(proxyURL = null),
            apiSourceProvider = null,
        )
        enqueue(endpoint.getPath(), expectedResult = HTTPResult.createResult())

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `performRequest bypasses the API source provider when the base URL is not the default host`() {
        val endpoint = Endpoint.GetCustomerInfo("test_user_id")
        // An overridden/proxy base URL pins the host; the API source (a different server) must not be used.
        val apiSourceServer = MockWebServer()
        val provider = FakeAPISourceProvider(listOf(apiSourceServer.url("/").toString()))
        val client = createClient(apiSourceProvider = provider)
        enqueue(endpoint.getPath(), expectedResult = HTTPResult.createResult())

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(apiSourceServer.requestCount).isEqualTo(0)
        apiSourceServer.shutdown()
    }

    @Test
    fun `performRequest ETag retry targets the same API source host`() {
        val endpoint = Endpoint.GetCustomerInfo("test_user_id")
        // GetCustomerInfo has no endpoint fallback URL, isolating API-source host resolution.
        assert(!endpoint.supportsFallbackBaseURLs)

        val provider = FakeAPISourceProvider(listOf(server.url("/").toString()))
        val client = createClient(
            appConfig = createAppConfig(proxyURL = null),
            apiSourceProvider = provider,
        )

        val urlString = server.url("/v1/subscribers/test_user_id").toString()

        // First attempt: an ETag cache miss (null result) forces a refresh retry.
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns null

        val expectedResult = HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS)
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                payload = expectedResult.payloadText,
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = true,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns expectedResult

        server.enqueue(MockResponse().setResponseCode(RCHTTPStatusCodes.NOT_MODIFIED))
        server.enqueue(
            MockResponse()
                .setResponseCode(expectedResult.responseCode)
                .setBody(expectedResult.payloadText),
        )

        val result = client.performRequest(
            URL(AppConfig.baseUrlString),
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )

        // Both the initial request and the ETag refresh retry must hit the API source host resolved from the
        // provider, not fall back to AppConfig.baseUrlString. If the retry ignored the API source, this server
        // would only see one request.
        val firstRequest = server.takeRequest()
        val secondRequest = server.takeRequest()
        assertThat(firstRequest.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(secondRequest.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
    }

    /** A minimal [RemoteConfigSourceProvider] whose current API source is the first of [urls]. */
    private class FakeAPISourceProvider(urls: List<String>) : RemoteConfigSourceProvider {
        private val handles = urls.mapIndexed { index, url ->
            RemoteConfigSourceHandle(
                purpose = RemoteConfigSourceHandle.Purpose.API,
                source = RemoteConfigSource(url = url, priority = index, weight = 1),
                token = index,
            )
        }

        override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
            handles.firstOrNull()

        override fun reportUnhealthy(handle: RemoteConfigSourceHandle) = Unit

        override fun restart(purpose: RemoteConfigSourceHandle.Purpose) = Unit

        override fun restartIfExhausted(purpose: RemoteConfigSourceHandle.Purpose): Boolean = false
    }

    // endregion

    @Test
    fun forwardsTheResponseCode() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = 223)
        )

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(223, "{'response': 'OK'}")
        )

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    @Test
    fun `GetRemoteConfig sends the RC format Accept header, skips ETags, and exposes an RC Format payload`() {
        val endpoint = Endpoint.GetRemoteConfig("app")
        val containerBytes = byteArrayOf('R'.code.toByte(), 'C'.code.toByte(), 1, 0, 0, 0, 0, 0)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(containerBytes)))

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.getHeader("Accept")).isEqualTo("application/x-rc-format")
        // Advertise the per-element codecs the SDK can decode so the server never sends brotli/zstd.
        assertThat(request.getHeader("Accept-RC-Element-Encoding")).isEqualTo("gzip")
        // RC Format endpoints are not ETag-cached: no If-None-Match is sent and the cache is bypassed.
        assertThat(request.getHeader(HTTPRequest.ETAG_HEADER_NAME)).isNull()
        verify(exactly = 0) {
            mockETagManager.getHTTPResultFromCacheOrBackend(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        assertThat(result.payload).isInstanceOf(HTTPResult.Payload.RCFormat::class.java)
        assertThat((result.payload as HTTPResult.Payload.RCFormat).bytes).isEqualTo(containerBytes)
    }

    @Test
    fun `GetRemoteConfig with a 204 and a missing body stream returns an empty payload instead of throwing`() {
        // On some Android devices a 204 yields no readable body stream (getInputStream returns null).
        // This must surface as a 204 with an empty payload, not as a network IOException.
        val spyClient = spyk(client)
        every { spyClient.getInputStream(any()) } returns null
        server.enqueue(MockResponse().setResponseCode(RCHTTPStatusCodes.NO_CONTENT))

        val result = spyClient.performRequest(
            baseURL,
            Endpoint.GetRemoteConfig("app"),
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )

        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.NO_CONTENT)
        assertThat(result.payload).isInstanceOf(HTTPResult.Payload.RCFormat::class.java)
        assertThat((result.payload as HTTPResult.Payload.RCFormat).bytes).isEmpty()
    }

    @Test(expected = IOException::class)
    fun `a non-204 response with a missing body stream still throws`() {
        val spyClient = spyk(client)
        every { spyClient.getInputStream(any()) } returns null
        server.enqueue(MockResponse().setResponseCode(200))

        spyClient.performRequest(
            baseURL,
            Endpoint.GetRemoteConfig("app"),
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
        )
    }

    @Test
    fun `non-RC-Format endpoints do not send the RC format Accept header`() {
        enqueue(
            Endpoint.LogIn.getPath(),
            expectedResult = HTTPResult.createResult(),
        )

        client.performRequest(baseURL, Endpoint.LogIn, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.getHeader("Accept")).isNotEqualTo("application/x-rc-format")
    }

    // region forceServerErrors

    @Test
    fun `when forceServerErrorsStrategy returns true, error url is used`() {
        val client = createClient(
            forceServerErrorStrategy = object : ForceServerErrorStrategy {
                override val serverErrorURL: String
                    get() = server.url("force-server-error").toString()
                override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                    return true
                }
            },
        )

        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = 502, payload = "Some error xml")
        )
        enqueue(
            "force-server-error",
            expectedResult = HTTPResult.createResult(responseCode = 502, payload = "Some error xml")
        )

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.requestUrl?.toString()).isEqualTo("${server.url("")}force-server-error")

        assertThat(result.responseCode).isEqualTo(502)
        assertThat(result.payloadText).isEqualTo("Some error xml")
    }

    @Test
    fun `when forceServerErrorsStrategy returns false, original url is used`() {
        val client = createClient(
            forceServerErrorStrategy = object : ForceServerErrorStrategy {
                override val serverErrorURL: String
                    get() = server.url("force-server-error").toString()
                override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean {
                    return false
                }
            },
        )

        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(223, "{'response': 'OK'}")
        )

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.requestUrl?.toString()).isEqualTo("${server.url("")}v1/subscribers/identify")

        assertThat(result.responseCode).isEqualTo(223)
        assertThat(result.payloadText).isEqualTo("{'response': 'OK'}")
    }

    // endregion forceServerErrors

    // Headers
    @Test
    fun addsHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        client = createClient(
            localeProvider = FakeLocaleProvider("en-US", "ja-JP"),
        )
        val expectedPreferredLocales = "en_US, ja_JP"
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(request.getHeader("X-Platform-Device")).isEqualTo(Build.MODEL)
        assertThat(request.getHeader("X-Platform-Brand")).isEqualTo(Build.BRAND)
        assertThat(request.getHeader("X-Platform-Flavor")).isEqualTo(expectedPlatformInfo.flavor)
        assertThat(request.getHeader("X-Platform-Flavor-Version")).isEqualTo(expectedPlatformInfo.version)
        assertThat(request.getHeader("X-Version")).isEqualTo(Config.frameworkVersion)
        assertThat(request.getHeader("X-Preferred-Locales")).isEqualTo(expectedPreferredLocales)
        assertThat(request.getHeader("X-Client-Locale")).isEqualTo("en-US")
        assertThat(request.getHeader("X-Client-Version")).isEqualTo("")
        assertThat(request.getHeader("X-Client-Bundle-ID")).isEqualTo("mock-package-name")
        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("false")
        assertThat(request.getHeader("X-Storefront")).isEqualTo("JP")
        assertThat(request.getHeader("X-Is-Debug-Build")).isEqualTo("false")
        assertThat(request.getHeader("X-Kotlin-Version")).isEqualTo(KotlinVersion.CURRENT.toString())
        assertThat(request.getHeader("X-Is-Backgrounded")).isEqualTo("true")
        assertThat(request.getHeader("X-Billing-Client-Sdk-Version")).isEqualTo(BuildConfig.BILLING_CLIENT_VERSION)
    }

    @Test
    fun `does not add custom entitlement computation header if disabled`() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.headers.names()).doesNotContain("X-Custom-Entitlements-Computation")
    }

    @Test
    fun `does not add storefront header if not cached`() {
        every { mockStorefrontProvider.getStorefront() } returns null
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.headers.names()).doesNotContain("X-Storefront")
    }

    @Test
    fun `adds custom entitlement computation header if enabled`() {
        client = createClient(appConfig = createAppConfig(customEntitlementComputation = true))
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Custom-Entitlements-Computation")).isEqualTo("true")
    }

    @Test
    fun `does not add UI preview mode header if disabled`() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.headers.names()).doesNotContain("X-UI-Preview-Mode")
    }

    @Test
    fun `adds UI preview mode header if enabled`() {
        client = createClient(appConfig = createAppConfig(uiPreviewMode = true))
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-UI-Preview-Mode")).isEqualTo("true")
    }

    @Test
    fun addsETagHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn

        every {
            mockETagManager.getETagHeaders(any(), any(), any())
        } answers {
            mapOf(
                HTTPRequest.ETAG_HEADER_NAME to "mock-etag",
                HTTPRequest.ETAG_LAST_REFRESH_NAME to "1234567890"
            )
        }

        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader(HTTPRequest.ETAG_HEADER_NAME)).isEqualTo("mock-etag")
        assertThat(request.getHeader(HTTPRequest.ETAG_LAST_REFRESH_NAME)).isEqualTo("1234567890")
    }

    @Test
    fun doesNotAddNullETagHeadersToRequest() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn

        every {
            mockETagManager.getETagHeaders(any(), any(), any())
        } answers {
            mapOf(
                HTTPRequest.ETAG_HEADER_NAME to "mock-etag",
                HTTPRequest.ETAG_LAST_REFRESH_NAME to null
            )
        }

        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

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
            endpoint.getPath(),
            expectedResult
        )

        client = createClient(appConfig)
        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        client.performRequest(baseURL, endpoint, body, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
        assertThat(request.getHeader(HTTPRequest.POST_PARAMS_HASH)).isNull()
    }

    @Test
    fun `given observer mode is enabled, observer mode header is sent`() {
        val appConfig = createAppConfig()
        appConfig.finishTransactions = false

        client = createClient(appConfig = appConfig)

        val expectedResult = HTTPResult.createResult()
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            expectedResult
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("true")
    }

    @Test
    fun `correctly sets debug header`() {
        val appConfig = createAppConfig(isDebugBuild = true)
        client = createClient(appConfig = appConfig)
        val endpoint = Endpoint.LogIn
        enqueue(
            endpoint.getPath(),
            HTTPResult.createResult()
        )

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))
        val request = server.takeRequest()

        assertThat(request.getHeader("X-Is-Debug-Build")).isEqualTo("true")
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
                .setBody(expectedResult.payloadText)

        server.enqueue(response)
        server.enqueue(secondResponse)

        val endpoint = Endpoint.LogIn
        val urlPathWithVersion = "/v1/subscribers/identify"
        val urlString = server.url(urlPathWithVersion).toString()
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns null

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                payload = expectedResult.payloadText,
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = true,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns expectedResult

        val result = client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        server.takeRequest()
        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeaders(any(), any(), refreshETag = false)
        }
        verify(exactly = 1) {
            mockETagManager.getETagHeaders(any(), any(), refreshETag = true)
        }
        assertThat(result.payloadText).isEqualTo(expectedResult.payloadText)
        assertThat(result.responseCode).isEqualTo(expectedResult.responseCode)
    }

    @Test
    fun `performRequest does not add nonce header to request if verification mode disabled`() {
        val endpoint = Endpoint.GetCustomerInfo("test-user-id")
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(verificationResult = VerificationResult.VERIFIED)
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            requestHeaders = emptyMap()
        )

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("X-Nonce")).isNull()
    }

    @Test
    fun `performRequest uses request time header if present when getting result from etag cache`() {
        val endpoint = Endpoint.LogIn
        enqueue(
            urlPath = endpoint.getPath(),
            expectedResult = HTTPResult.createResult(),
            requestDateHeader = Date(1234567890)
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
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
                VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        }
    }

    @Test
    fun `payload is returned with trailing new lines`() {
        enqueue(
            Endpoint.LogIn.getPath(),
            expectedResult = HTTPResult.createResult(payload = "{}\n")
        )

        val result = client.performRequest(baseURL, Endpoint.LogIn, body = null, postFieldsToSign = null, emptyMap())

        server.takeRequest()

        assertThat(result.payloadText).isEqualTo("{}\n")
    }

    // region trackHttpRequestPerformed

    @Test
    fun `performRequest tracks http request performed diagnostic event if request successful`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = 200
        val requestStartTime = 1676379370000L // Tuesday, February 14, 2023 12:56:10:000 PM GMT
        val requestEndTime = 1676379370123L // Tuesday, February 14, 2023 12:56:10:123 PM GMT
        val responseTime = (requestEndTime - requestStartTime).milliseconds

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult()
        )

        every { dateProvider.now } returnsMany listOf(Date(requestStartTime), Date(requestEndTime))

        client.performRequest(baseURL, Endpoint.LogIn, body = null, postFieldsToSign = null, mapOf("" to ""))
        server.takeRequest()

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                server.hostName,
                endpoint,
                responseTime,
                wasSuccessful = true,
                responseCode,
                backendErrorCode = null,
                HTTPResult.Origin.BACKEND,
                VerificationResult.NOT_REQUESTED,
                isRetry = false,
                connectionErrorReason = null,
            )
        }
    }

    @Test
    fun `performRequest tracks http request performed diagnostic event if request fails`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = RCHTTPStatusCodes.BAD_REQUEST
        val requestStartTime = 1676379370000L // Tuesday, February 14, 2023 12:56:10:000 PM GMT
        val requestEndTime = 1676379370123L // Tuesday, February 14, 2023 12:56:10:123 PM GMT
        val responseTime = (requestEndTime - requestStartTime).milliseconds
        val backendErrorCode = 1234

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = responseCode, payload = "{\"code\":$backendErrorCode}")
        )

        every { dateProvider.now } returnsMany listOf(Date(requestStartTime), Date(requestEndTime))

        client.performRequest(baseURL, Endpoint.LogIn, body = null, postFieldsToSign = null, mapOf("" to ""))
        server.takeRequest()

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                server.hostName,
                endpoint,
                responseTime,
                wasSuccessful = false,
                responseCode,
                backendErrorCode,
                HTTPResult.Origin.BACKEND,
                VerificationResult.NOT_REQUESTED,
                isRetry = false,
                connectionErrorReason = null,
            )
        }
    }

    @Test
    fun `performRequest tracks http request performed diagnostic event if request throws Exception`() {
        val dateProvider = mockk<DateProvider>()
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        every { dateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT
        client = createClient(diagnosticsTracker = diagnosticsTracker, dateProvider = dateProvider)

        val endpoint = Endpoint.LogIn
        val responseCode = RCHTTPStatusCodes.BAD_REQUEST

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.BAD_REQUEST,
                "not uh json",
                eTagHeader = any(),
                urlString = server.url(endpoint.getPath()).toString(),
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } throws JSONException("bad json")
        val response = MockResponse().setBody("not uh json").setResponseCode(responseCode)
        server.enqueue(response)

        try {
            client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))
        } catch (e: JSONException) {
            verify(exactly = 1) {
                diagnosticsTracker.trackHttpRequestPerformed(
                    server.hostName,
                    endpoint,
                    responseTime = any(),
                    wasSuccessful = false,
                    HTTPClient.NO_STATUS_CODE,
                    backendErrorCode = null,
                    resultOrigin = null,
                    VerificationResult.NOT_REQUESTED,
                    isRetry = false,
                    connectionErrorReason = null,
                )
            }
            return
        }
        error("Expected exception")
    }

    @Test
    fun `if there's an error getting ETag, retry call passes track diagnostics parameter isRetry to true`() {
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        client = createClient(diagnosticsTracker = diagnosticsTracker)

        val response =
            MockResponse()
                .setHeader(HTTPResult.ETAG_HEADER_NAME, "anetag")
                .setResponseCode(RCHTTPStatusCodes.NOT_MODIFIED)

        val expectedResult = HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse)
        val secondResponse =
            MockResponse()
                .setHeader(HTTPResult.ETAG_HEADER_NAME, "anotheretag")
                .setResponseCode(expectedResult.responseCode)
                .setBody(expectedResult.payloadText)

        server.enqueue(response)
        server.enqueue(secondResponse)

        val endpoint = Endpoint.LogIn
        val urlPathWithVersion = "/v1/subscribers/identify"
        val urlString = server.url(urlPathWithVersion).toString()
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = false,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns null

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                payload = expectedResult.payloadText,
                eTagHeader = any(),
                urlString = urlString,
                refreshETag = true,
                requestDate = null,
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns expectedResult

        client.performRequest(baseURL, endpoint, body = null, postFieldsToSign = null, mapOf("" to ""))

        server.takeRequest()
        server.takeRequest()

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                server.hostName,
                endpoint,
                any(),
                true,
                RCHTTPStatusCodes.SUCCESS,
                null,
                HTTPResult.Origin.BACKEND,
                VerificationResult.NOT_REQUESTED,
                isRetry = true,
                connectionErrorReason = null,
            )
        }
    }

    // endregion

    // region Fallback API host

    @Test
    fun `performRequest retries call with fallback API host if server returns 500`() {
        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(),
            server = fallbackServer,
            isFallbackURL = true,
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        val request0 = server.takeRequest()
        assertThat(request0.method).isEqualTo("GET")
        assertThat(request0.path).isEqualTo("/v1/subscribers/test_user_id/offerings")

        assertThat(server.requestCount).isEqualTo(1)

        val request1 = fallbackServer.takeRequest()
        assertThat(request1.method).isEqualTo("GET")
        assertThat(request1.path).isEqualTo("/v1/offerings")
    }

    @Test
    fun `performRequest does not retry call with fallback API host if server returns non-500 error`() {
        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.NOT_FOUND

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(),
            server = fallbackServer,
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/v1/subscribers/test_user_id/offerings")
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(fallbackServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `performRequest does not retry call with fallback API host if endpoint does not support fallback base URLs`() {
        // This test requires an endpoint that does not support fallback host URLs
        val endpoint = Endpoint.GetCustomerInfo("test_user_id")
        assert(!endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(),
            server = fallbackServer,
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/v1/subscribers/test_user_id")
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(fallbackServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `performRequest returns failed response of fallback request if main server returns 500`() {
        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(responseCode = RCHTTPStatusCodes.NOT_FOUND),
            server = fallbackServer,
            isFallbackURL = true,
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        assertThat(result.responseCode).`as`("response code is 404").isEqualTo(404)
    }

    @Test
    fun `performRequest returns failed response (500) if both the main server and fallback server return 500`() {
        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode),
            server = fallbackServer,
            isFallbackURL = true,
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        assertThat(server.requestCount).isEqualTo(1)
        assertThat(fallbackServer.requestCount).isEqualTo(1)
        assertThat(result.responseCode).`as`("response code is 500").isEqualTo(500)
    }

    @Test
    fun `performRequest returns successful response of fallback request if main server returns 500`() {
        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetProductEntitlementMapping
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(223, "{'response': 'OK'}"),
            server = fallbackServer,
            isFallbackURL = true,
        )

        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
        assertThat(result.body.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    @Test
    fun `if performRequest uses a fallback host URL, then the correct track diagnostics calls happen`() {
        val diagnosticsTracker = mockk<DiagnosticsTracker>()
        every { diagnosticsTracker.trackHttpRequestPerformed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        client = createClient(diagnosticsTracker = diagnosticsTracker)

        // This test requires an endpoint that supports fallback host URLs
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()

        val serverDownResponseCode = RCHTTPStatusCodes.ERROR

        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(responseCode = serverDownResponseCode)
        )

        enqueue(
            endpoint.getPath(useFallback = true),
            expectedResult = HTTPResult.createResult(responseCode = RCHTTPStatusCodes.SUCCESS),
            server = fallbackServer,
            isFallbackURL = true,
        )

        client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = listOf(fallbackBaseURL),
        )

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                server.hostName,
                endpoint,
                any(),
                false,
                RCHTTPStatusCodes.ERROR,
                null,
                HTTPResult.Origin.BACKEND,
                VerificationResult.NOT_REQUESTED,
                isRetry = false,
                connectionErrorReason = null,
            )
        }

        verify(exactly = 1) {
            diagnosticsTracker.trackHttpRequestPerformed(
                fallbackServer.hostName,
                endpoint,
                any(),
                true,
                RCHTTPStatusCodes.SUCCESS,
                null,
                HTTPResult.Origin.BACKEND,
                VerificationResult.NOT_REQUESTED,
                isRetry = false,
                connectionErrorReason = null,
            )
        }
    }

    // region Timeout Management

    @Test
    fun `HTTPClient records SUCCESS_ON_MAIN_BACKEND when successful request to main backend`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")

        val appConfig = createAppConfig()
        val timeoutManager = spyk(HTTPTimeoutManager(appConfig))

        // Create app config with main backend URL
        client = createClient(appConfig = appConfig, timeoutManager = timeoutManager)

        // Record a timeout first to verify it gets reset
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)

        // Setup successful response
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, """{"offerings": [], "current_offering_id": null}""")
        )

        verify(exactly = 0) {
            timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        }

        // Perform request to main backend
        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = emptyList(),
        )

        // Verify timeout was reset
        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
        verify(exactly = 1) {
            timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.SUCCESS_ON_MAIN_BACKEND)
        }
    }

    @Test
    fun `HTTPClient records TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT when timeout occurs on main backend with fallback`() {
        val endpoint = Endpoint.GetOfferings("test_user_id")
        assert(endpoint.supportsFallbackBaseURLs)

        val appConfig = createAppConfig()
        val timeoutManager = spyk(HTTPTimeoutManager(appConfig))

        // Create app config with main backend URL
        client = createClient(appConfig = appConfig, timeoutManager = timeoutManager)

        // Initially timeout should be default
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)

        // Setup fallback server response
        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()
        val validJsonPayload = """{"offerings": [], "current_offering_id": null}"""
        fallbackServer.enqueue(
            MockResponse()
                .setBody(validJsonPayload)
                .setResponseCode(RCHTTPStatusCodes.SUCCESS)
        )

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.SUCCESS,
                validJsonPayload,
                eTagHeader = any(),
                urlString = any(),
                refreshETag = false,
                requestDate = any(),
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = true,
            )
        } returns HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, validJsonPayload)

        enqueue(
            endpoint.getPath(),
            HTTPResult.createResult(),
        )

        verify(exactly = 0) {
            timeoutManager.recordRequestResult(any())
        }

        try {
            // Perform request - should timeout on main backend and use fallback
            val result = client.performRequest(
                URL("http://10.255.255.255/"), // Unroutable IP to force connection timeout
                endpoint,
                body = null,
                postFieldsToSign = null,
                mapOf("" to ""),
                fallbackBaseURLs = listOf(fallbackBaseURL),
            )

            // Verify HTTPClient recorded TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
            assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
            assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
                .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
            verify(exactly = 1) {
                timeoutManager.recordRequestResult(
                    HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
                )
            }
        } finally {
            fallbackServer.shutdown()
        }
    }

    @Test
    fun `HTTPClient records OTHER_RESULT when timeout occurs on main backend with endpoint not supporting fallback`() {
        val endpoint = Endpoint.LogIn

        val appConfig = createAppConfig()
        val timeoutManager = spyk(HTTPTimeoutManager(appConfig))

        // Create app config with main backend URL
        client = createClient(appConfig = appConfig, timeoutManager = timeoutManager)

        // Initially timeout should be default
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)

        enqueue(
            endpoint.getPath(),
            HTTPResult.createResult(),
        )

        verify(exactly = 0) {
            timeoutManager.recordRequestResult(any())
        }

        // Perform request - should timeout on main backend and not use fallback
        assertThatThrownBy {
            client.performRequest(
                URL("http://10.255.255.255/"), // Unroutable IP to force connection timeout
                endpoint,
                body = null,
                postFieldsToSign = null,
                mapOf("" to ""),
                fallbackBaseURLs = emptyList(),
            )
        }.isInstanceOf(SocketTimeoutException::class.java)

        // Verify HTTPClient recorded OTHER_RESULT (not timeout for fallback endpoint)
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = false))
            .isEqualTo(HTTPTimeoutManager.DEFAULT_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.SUPPORTED_FALLBACK_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
        verify(exactly = 1) {
            timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.OTHER_RESULT)
        }
    }

    @Test
    fun `HTTPClient records OTHER_RESULT when request fails without timeout`() {
        val endpoint = Endpoint.GetProductEntitlementMapping

        val appConfig = createAppConfig()
        val timeoutManager = spyk(HTTPTimeoutManager(appConfig))

        // Create app config with main backend URL
        client = createClient(appConfig = appConfig, timeoutManager = timeoutManager)

        // Record a timeout first
        timeoutManager.recordRequestResult(
            HTTPTimeoutManager.RequestResult.TIMEOUT_ON_MAIN_BACKEND_FOR_FALLBACK_SUPPORTED_ENDPOINT
        )
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)

        // Setup error response (non-timeout error)
        enqueue(
            endpoint.getPath(),
            expectedResult = HTTPResult.createResult(RCHTTPStatusCodes.NOT_FOUND, """{"error": "not found"}""")
        )

        verify(exactly = 0) {
            timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.OTHER_RESULT)
        }

        // Perform request - should record OTHER_RESULT for non-successful response
        val result = client.performRequest(
            baseURL,
            endpoint,
            body = null,
            postFieldsToSign = null,
            mapOf("" to ""),
            fallbackBaseURLs = emptyList(),
        )

        // Verify HTTPClient recorded OTHER_RESULT and did NOT reset timeout
        assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.NOT_FOUND)
        assertThat(timeoutManager.getTimeoutForRequest(isFallback = false, fallbackAvailable = true))
            .isEqualTo(HTTPTimeoutManager.REDUCED_TIMEOUT_MS / HTTPTimeoutManager.TEST_DIVIDER)
        verify(exactly = 1) {
            timeoutManager.recordRequestResult(HTTPTimeoutManager.RequestResult.OTHER_RESULT)
        }
    }

    // endregion Timeout Management
}

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ParameterizedNonJsonResponseBodyTest(
    private val endpoint: Endpoint,
    private val statusCode: Int,
) : BaseHTTPClientTest() {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "endpoint={0}, statusCode={1}")
        fun parameters(): Collection<Array<Any>> {
            return listOf(
                arrayOf(Endpoint.GetOfferings("test_user"), 500),
                arrayOf(Endpoint.GetOfferings("test_user"), 503),
                arrayOf(Endpoint.GetOfferings("test_user"), 504),
                arrayOf(Endpoint.GetProductEntitlementMapping, 500),
                arrayOf(Endpoint.GetProductEntitlementMapping, 503),
                arrayOf(Endpoint.GetProductEntitlementMapping, 504),
            )
        }
    }

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        client = createClient()
    }

    @Test
    fun `performRequest should retry with fallback URL when server returns non-JSON response`() {
        // Arrange
        assert(endpoint.supportsFallbackBaseURLs) {
            "This test is only meant to test endpoints supporting fallback URLs."
        }
        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()
        val invalidJsonPayload = "<html><body>504 Gateway Timeout</body></html>"
        val validJsonPayload = """{"offerings": [], "current_offering_id": null}"""
        val mainResponse = MockResponse()
            .setBody(invalidJsonPayload)
            .setResponseCode(statusCode)
        val fallbackResponse = MockResponse()
            .setBody(validJsonPayload)
            .setResponseCode(RCHTTPStatusCodes.SUCCESS)
        server.enqueue(mainResponse)
        fallbackServer.enqueue(fallbackResponse)
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                statusCode,
                invalidJsonPayload,
                eTagHeader = any(),
                urlString = server.url(endpoint.getPath()).toString(),
                refreshETag = false,
                requestDate = any(),
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns HTTPResult.createResult(statusCode, invalidJsonPayload)
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.SUCCESS,
                validJsonPayload,
                eTagHeader = any(),
                urlString = fallbackServer.url(endpoint.getPath(useFallback = true)).toString(),
                refreshETag = false,
                requestDate = any(),
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = true,
            )
        } returns HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, validJsonPayload)

        // Act
        try {
            val result = client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                mapOf("" to ""),
                fallbackBaseURLs = listOf(fallbackBaseURL),
            )

            // Assert
            assertThat(server.requestCount).isEqualTo(1)
            assertThat(fallbackServer.requestCount).isEqualTo(1)
            assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
            assertThat(result.payloadText).isEqualTo(validJsonPayload)
            assertThat(result.body.has("offerings")).isTrue
        } finally {
            fallbackServer.shutdown()
        }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ParameterizedConnectionFailureFallbackTest(
    private val endpoint: Endpoint,
) : BaseHTTPClientTest() {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "endpoint={0}")
        fun parameters(): Collection<Array<Any>> {
            return listOf(
                arrayOf(Endpoint.GetOfferings("test_user")),
                arrayOf(Endpoint.GetProductEntitlementMapping),
            )
        }
    }

    @Before
    fun setupClient() {
        mockSigningManager = mockk()
        every { mockSigningManager.shouldVerifyEndpoint(any()) } returns false
        client = createClient()
    }

    @Test
    fun `performRequest should retry with fallback URL when connection fails`() {
        // Arrange
        assert(endpoint.supportsFallbackBaseURLs) {
            "This test is only meant to test endpoints supporting fallback URLs."
        }
        val fallbackServer = MockWebServer()
        val fallbackBaseURL = fallbackServer.url("/v1").toUrl()
        val validJsonPayload = """{"offerings": [], "current_offering_id": null}"""
        // Shut down main server to cause IOException when client tries to connect
        server.shutdown()
        val fallbackResponse = MockResponse()
            .setBody(validJsonPayload)
            .setResponseCode(RCHTTPStatusCodes.SUCCESS)
        fallbackServer.enqueue(fallbackResponse)
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.SUCCESS,
                validJsonPayload,
                eTagHeader = any(),
                urlString = server.url(endpoint.getPath()).toString(),
                refreshETag = false,
                requestDate = any(),
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
            )
        } returns HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, validJsonPayload)

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.SUCCESS,
                validJsonPayload,
                eTagHeader = any(),
                urlString = fallbackServer.url(endpoint.getPath(useFallback = true)).toString(),
                refreshETag = false,
                requestDate = any(),
                verificationResult = VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = true,
            )
        } returns HTTPResult.createResult(RCHTTPStatusCodes.SUCCESS, validJsonPayload)

        // Act
        try {
            val result = client.performRequest(
                baseURL,
                endpoint,
                body = null,
                postFieldsToSign = null,
                mapOf("" to ""),
                fallbackBaseURLs = listOf(fallbackBaseURL),
            )

            // Assert
            assertThat(fallbackServer.requestCount).isEqualTo(1)
            assertThat(result.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
            assertThat(result.payloadText).isEqualTo(validJsonPayload)
            assertThat(result.body.has("offerings")).isTrue
        } finally {
            fallbackServer.shutdown()
        }
    }
}
