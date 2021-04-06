//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPRequest
import com.revenuecat.purchases.common.networking.HTTPResult
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

    companion object {

        @JvmStatic
        private lateinit var server: MockWebServer
        @JvmStatic
        private lateinit var baseURL: URL

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            baseURL = server.url("/v1").toUrl()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            server.shutdown()
        }
    }

    private lateinit var appConfig: AppConfig
    private val mockETagManager = mockk<ETagManager>().also {
        val httpRequestSlot = slot<HTTPRequest>()
        every {
            it.addETagHeaderToRequest(capture(httpRequestSlot))
        } answers {
            val capturedHTTPRequest = httpRequestSlot.captured
            val updatedHeaders = capturedHTTPRequest.headers + mapOf("X-RevenueCat-ETag" to "etag")
            capturedHTTPRequest.copy(headers = updatedHeaders)
        }
    }
    private val expectedPlatformInfo = PlatformInfo("flutter", "2.1.0")
    private lateinit var client: HTTPClient

    @Before
    fun setupBefore() {
        appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = expectedPlatformInfo,
            proxyURL = baseURL,
            store = Store.PLAY_STORE
        )
        client = HTTPClient(appConfig, mockETagManager)
    }


    @Test
    fun canPerformASimpleGet() {
        mockResponse(MockResponse().setBody("{}"))

        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method request is GET").isEqualTo("GET")
        assertThat(request.path).`as`("method path is /v1/resource").isEqualTo("/v1/resource")
    }

    @Test
    fun forwardsTheResponseCode() {
        mockResponse(MockResponse().setBody("{}").setResponseCode(223))

        val result = client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        mockResponse(MockResponse().setBody("{'response': 'OK'}").setResponseCode(223))

        val result = client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val response = MockResponse().setBody("not uh jason")
        server.enqueue(response)

        try {
            client.performRequest("/resource", null, mapOf("" to ""))
        } finally {
            server.takeRequest()
        }
    }

    // Headers
    @Test
    fun addsHeadersToRequest() {
        mockResponse(MockResponse().setBody("{}"))

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        client.performRequest("/resource", null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        mockResponse(MockResponse().setBody("{}"))

        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(request.getHeader("X-Platform-Flavor")).isEqualTo(expectedPlatformInfo.flavor)
        assertThat(request.getHeader("X-Platform-Flavor-Version")).isEqualTo(expectedPlatformInfo.version)
        assertThat(request.getHeader("X-Version")).isEqualTo(Config.frameworkVersion)
        assertThat(request.getHeader("X-Client-Locale")).isEqualTo(appConfig.languageTag)
        assertThat(request.getHeader("X-Client-Version")).isEqualTo(appConfig.versionName)
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
        mockResponse(MockResponse().setBody("{}"))

        client = HTTPClient(appConfig, mockETagManager)
        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        mockResponse(MockResponse().setBody("{}"))

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        client.performRequest("/resource", body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
    }

    @Test
    fun `given observer mode is enabled, observer mode header is sent`() {
        appConfig.finishTransactions = false
        mockResponse(MockResponse().setBody("{}"))

        client.performRequest("/resource", null, mapOf("" to ""))

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

    private fun mockResponse(response: MockResponse) {
        server.enqueue(response)
        val lst = slot<HTTPResult>()
        every {
            mockETagManager.processResponse(any(), any(), capture(lst))
        } answers {
            lst.captured
        }
    }
}
