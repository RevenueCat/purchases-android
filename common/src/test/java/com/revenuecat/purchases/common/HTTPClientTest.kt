//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.networking.ETAG_HEADER_NAME
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.HashMap
import org.robolectric.annotation.Config as AnnotationConfig

@RunWith(AndroidJUnit4::class)
@AnnotationConfig(manifest = AnnotationConfig.NONE)
class HTTPClientTest {

    private lateinit var server: MockWebServer
    private lateinit var baseURL: URL

    @Before
    fun setup() {
        server = MockWebServer()
        baseURL = server.url("/v1").toUrl()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private lateinit var appConfig: AppConfig

    private val mockedETags = emptyMap<String, String>()
    private val mockETagManager = mockk<ETagManager>().also {
        val pathSlot = slot<String>()
        every {
            it.getETagHeader(capture(pathSlot), any())
        } answers {
            val capturedPath = pathSlot.captured
            mapOf(ETAG_HEADER_NAME to (mockedETags[capturedPath] ?: ""))
        }
    }
    private val expectedPlatformInfo = PlatformInfo("flutter", "2.1.0")
    private lateinit var client: HTTPClient

    @Before
    fun setupBefore() {
        val context = mockk<Context>(relaxed = true).apply {
            every { packageName } answers { "mock-package-name" }
        }
        appConfig = AppConfig(
            context = context,
            observerMode = false,
            platformInfo = expectedPlatformInfo,
            proxyURL = baseURL,
            store = Store.PLAY_STORE
        )
        client = HTTPClient(appConfig, mockETagManager)
    }

    @Test
    fun canPerformASimpleGet() {
        val path = "/resource"
        enqueue(
            path,
            expectedResult = HTTPResult(200, "{}")
        )

        client.performRequest(path, null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method request is GET").isEqualTo("GET")
        assertThat(request.path).`as`("method path is /v1/resource").isEqualTo("/v1/resource")
    }

    @Test
    fun forwardsTheResponseCode() {
        val path = "/resource"
        enqueue(
            path,
            expectedResult = HTTPResult(223, "{}")
        )

        val result = client.performRequest(path, null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val path = "/resource"
        enqueue(
            path,
            expectedResult = HTTPResult(223, "{'response': 'OK'}")
        )

        val result = client.performRequest(path, null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val path = "/resource"
        enqueue(
            path,
            expectedResult = HTTPResult(200, "not uh jason")
        )

        try {
            client.performRequest(path, null, mapOf("" to ""))
        } finally {
            server.takeRequest()
        }
    }

    // Headers
    @Test
    fun addsHeadersToRequest() {
        val expectedResult = HTTPResult(200, "{}")
        val path = "/resource"
        enqueue(
            path,
            expectedResult
        )

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        client.performRequest(path, null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        val expectedResult = HTTPResult(200, "{}")
        val path = "/resource"
        enqueue(
            path,
            expectedResult
        )

        client.performRequest(path, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(request.getHeader("X-Platform-Flavor")).isEqualTo(expectedPlatformInfo.flavor)
        assertThat(request.getHeader("X-Platform-Flavor-Version")).isEqualTo(expectedPlatformInfo.version)
        assertThat(request.getHeader("X-Version")).isEqualTo(Config.frameworkVersion)
        assertThat(request.getHeader("X-Client-Locale")).isEqualTo(appConfig.languageTag)
        assertThat(request.getHeader("X-Client-Version")).isEqualTo(appConfig.versionName)
        assertThat(request.getHeader("X-Client-Package-Name")).isEqualTo("mock-package-name")
        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("false")
    }

    @Test
    fun `Given there is no flavor version, flavor version header is not set`() {
        appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo("native", null),
            proxyURL = baseURL,
            store = Store.PLAY_STORE
        )

        val expectedResult = HTTPResult(200, "{}")
        val path = "/resource"
        enqueue(
            path,
            expectedResult
        )

        client = HTTPClient(appConfig, mockETagManager)
        client.performRequest(path, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        val expectedResult = HTTPResult(200, "{}")
        val path = "/resource"
        enqueue(
            path,
            expectedResult
        )

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        client.performRequest(path, body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
    }

    @Test
    fun `given observer mode is enabled, observer mode header is sent`() {
        appConfig.finishTransactions = false

        val expectedResult = HTTPResult(200, "{}")
        val path = "/resource"
        enqueue(
            path,
            expectedResult
        )

        client.performRequest(path, null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("true")
    }

    private fun enqueue(
        path: String,
        expectedResult: HTTPResult
    ) {
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                expectedResult.payload,
                connection = any(),
                "/v1$path",
                refreshETag = false
            )
        } returns expectedResult
        val response = MockResponse().setBody(expectedResult.payload).setResponseCode(expectedResult.responseCode)
        server.enqueue(response)
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
                .setHeader(ETAG_HEADER_NAME, "anetag")
                .setResponseCode(RCHTTPStatusCodes.NOT_MODIFIED)

        val expectedResult = HTTPResult(RCHTTPStatusCodes.SUCCESS, Responses.validEmptyPurchaserResponse)
        val secondResponse =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, "anotheretag")
                .setResponseCode(expectedResult.responseCode)
                .setBody(expectedResult.payload)

        server.enqueue(response)
        server.enqueue(secondResponse)

        val path = "/resource"
        val urlPathWithVersion = "/v1$path"
        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                RCHTTPStatusCodes.NOT_MODIFIED,
                payload = "",
                connection = any(),
                urlPathWithVersion,
                refreshETag = false
            )
        } returns null

        every {
            mockETagManager.getHTTPResultFromCacheOrBackend(
                expectedResult.responseCode,
                payload = expectedResult.payload,
                connection = any(),
                urlPathWithVersion,
                refreshETag = true
            )
        } returns expectedResult

        val result = client.performRequest(path, null, mapOf("" to ""))

        server.takeRequest()
        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), false)
        }
        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), true)
        }
        assertThat(result.payload).isEqualTo(expectedResult.payload)
        assertThat(result.responseCode).isEqualTo(expectedResult.responseCode)
    }


}
