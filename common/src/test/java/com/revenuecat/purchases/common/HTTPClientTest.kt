//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config as AnnotationConfig
import java.net.URL
import java.util.HashMap

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

    private val expectedPlatformInfo = PlatformInfo("flutter", "2.1.0")
    
    @Before
    fun setupBefore() {
        appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = expectedPlatformInfo,
            proxyURL = baseURL
        )
    }

    @Test
    fun canBeCreated() {
        HTTPClient(appConfig)
    }

    @Test
    fun canPerformASimpleGet() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        HTTPClient(appConfig)
            .apply {
                this.performRequest("/resource", null, mapOf("" to ""))
            }

        val request = server.takeRequest()
        assertThat(request.method).`as`("method request is GET").isEqualTo("GET")
        assertThat(request.path).`as`("method path is /v1/resource").isEqualTo("/v1/resource")
    }

    @Test
    fun forwardsTheResponseCode() {
        val response = MockResponse().setBody("{}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(appConfig)
        val result = client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val response = MockResponse().setBody("{'response': 'OK'}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(appConfig)
        val result = client.performRequest("/resource", null, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body!!.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val response = MockResponse().setBody("not uh jason")
        server.enqueue(response)

        val client = HTTPClient(appConfig)
        try {
            client.performRequest("/resource", null, mapOf("" to ""))
        } finally {
            server.takeRequest()
        }
    }

    // Headers
    @Test
    fun addsHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val headers = HashMap<String, String>()
        headers["Authentication"] = "Bearer todd"

        val client = HTTPClient(appConfig)
        client.performRequest("/resource", null, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(appConfig)
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
            proxyURL = baseURL
        )
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(appConfig)
        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Platform-Flavor-Version")).isNull()
    }

    @Test
    fun addsPostBody() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        val client = HTTPClient(appConfig)
        client.performRequest("/resource", body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
    }

    @Test
    fun `given observer mode is enabled, observer mode header is sent`() {
        appConfig.finishTransactions = false
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(appConfig)
        client.performRequest("/resource", null, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("X-Observer-Mode-Enabled")).isEqualTo("true")
    }

}
