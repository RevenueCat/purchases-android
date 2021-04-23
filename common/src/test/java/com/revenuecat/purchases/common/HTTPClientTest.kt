//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.networking.ETAG_HEADER_NAME
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.HTTPResult
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
        server.enqueue(MockResponse().setBody("{}"))

        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method request is GET").isEqualTo("GET")
        assertThat(request.path).`as`("method path is /v1/resource").isEqualTo("/v1/resource")
    }

    @Test
    fun forwardsTheResponseCode() {
        server.enqueue(MockResponse().setBody("{}").setResponseCode(223))

        val result = client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        server.enqueue(MockResponse().setBody("{'response': 'OK'}").setResponseCode(223))

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
        server.enqueue(MockResponse().setBody("{}"))

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        client.performRequest("/resource", null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        server.enqueue(MockResponse().setBody("{}"))

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
        server.enqueue(MockResponse().setBody("{}"))

        client = HTTPClient(appConfig, mockETagManager)
        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        server.enqueue(MockResponse().setBody("{}"))

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
        server.enqueue(MockResponse().setBody("{}"))

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

    @Test
    fun `if there's an error getting ETag, retry call refreshing ETags`() {
        val response =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, "anetag")
                .setResponseCode(304)

        val secondResponse =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, "anotheretag")
                .setBody(Responses.validEmptyPurchaserResponse)

        server.enqueue(response)
        server.enqueue(secondResponse)

        every {
            mockETagManager.storeBackendResultIfNoError(any(), any(), any())
        } just Runs
        every {
            mockETagManager.getStoredResult(any(), 304)
        } answers {
            null to true
        }

        every {
            mockETagManager.getStoredResult(any(), 200)
        } answers {
            null to false
        }

        client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()
        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), false)
        }
        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), true)
        }

    }

    @Test
    fun `store result from backend if result is not cached already`() {
        val eTagInResponse = "etagInResponse"
        val response =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, eTagInResponse)
                .setResponseCode(200)
                .setBody("{}")

        server.enqueue(response)

        val resultFromBackendSlot = slot<HTTPResult>()
        every {
            mockETagManager.storeBackendResultIfNoError(any(), capture(resultFromBackendSlot), any())
        } just Runs

        val pathSlot = slot<String>()
        every {
            mockETagManager.getStoredResult(capture(pathSlot), 200)
        } answers {
            null to false
        }

        client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), false)
        }
        assertThat(pathSlot.isCaptured).isTrue()
        assertThat(resultFromBackendSlot.isCaptured).isTrue()
        verify(exactly = 1) {
            mockETagManager.storeBackendResultIfNoError(pathSlot.captured, resultFromBackendSlot.captured, eTagInResponse)
        }
    }

    @Test
    fun `don't store result from backend if the response code is 304 and there is a cached result`() {
        val eTagInResponse = "etagInResponse"
        val response =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, eTagInResponse)
                .setResponseCode(304)

        server.enqueue(response)

        val cachedResult = HTTPResult(200, Responses.validEmptyPurchaserResponse)
        every {
            mockETagManager.getStoredResult(any(), 304)
        } answers {
            cachedResult to false
        }

        client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        verify(exactly = 0) {
            mockETagManager.storeBackendResultIfNoError(any(), any(), any())
        }
    }

    @Test
    fun `include API version in path when storing result from backend`() {
        val eTagInResponse = "etagInResponse"
        val response =
            MockResponse()
                .setHeader(ETAG_HEADER_NAME, eTagInResponse)
                .setResponseCode(200)
                .setBody("{}")

        server.enqueue(response)

        val pathWhenGettingStoredResult = slot<String>()
        every {
            mockETagManager.getStoredResult(capture(pathWhenGettingStoredResult), 200)
        } answers {
            null to false
        }

        val pathWhenStoringResult = slot<String>()
        every {
            mockETagManager.storeBackendResultIfNoError(capture(pathWhenStoringResult), any(), eTagInResponse)
        } just Runs

        val path = "/resource"
        val expectedPath = "/v1$path"
        client.performRequest(path, null, mapOf("" to ""))

        server.takeRequest()

        verify(exactly = 1) {
            mockETagManager.getETagHeader(any(), false)
        }

        assertThat(pathWhenStoringResult.isCaptured).isTrue()
        assertThat(pathWhenGettingStoredResult.isCaptured).isTrue()
        assertThat(pathWhenStoringResult.captured).isEqualTo(expectedPath)
        assertThat(pathWhenStoringResult.captured).isEqualTo(pathWhenGettingStoredResult.captured)
    }

}
