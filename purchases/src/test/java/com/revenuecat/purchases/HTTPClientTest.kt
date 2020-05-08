//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
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

    private val appConfig = AppConfig("en-US", "1.0", "native", "3.2.0")

    @Test
    fun canBeCreated() {
        HTTPClient(baseURL, appConfig)
    }

    @Test
    fun canPerformASimpleGet() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        HTTPClient(baseURL, appConfig)
            .apply {
                performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))
            }

        val request = server.takeRequest()
        assertThat(request.method).`as`("method request is GET").isEqualTo("GET")
        assertThat(request.path).`as`("method path is /v1/resource").isEqualTo("/v1/resource")
    }

    @Test
    fun forwardsTheResponseCode() {
        val response = MockResponse().setBody("{}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(baseURL, appConfig)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val response = MockResponse().setBody("{'response': 'OK'}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(baseURL, appConfig)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body!!.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val response = MockResponse().setBody("not uh jason")
        server.enqueue(response)

        val client = HTTPClient(baseURL, appConfig)
        try {
            client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))
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

        val client = HTTPClient(baseURL, appConfig)
        client.performRequest("/resource", null as Map<*, *>?, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(baseURL, appConfig)
        client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(request.getHeader("X-Platform-Flavor")).isEqualTo("native")
        assertThat(request.getHeader("X-Platform-Flavor-Version")).isEqualTo("3.2.0")
        assertThat(request.getHeader("X-Version")).isEqualTo(Purchases.frameworkVersion)
        assertThat(request.getHeader("X-Client-Locale")).isEqualTo(appConfig.languageTag)
        assertThat(request.getHeader("X-Client-Version")).isEqualTo(appConfig.versionName)
    }

    @Test
    fun addsPostBody() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        val client = HTTPClient(baseURL, appConfig)
        client.performRequest("/resource", body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size).isGreaterThan(0)
    }
}
