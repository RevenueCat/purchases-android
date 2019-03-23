//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

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
            baseURL = server.url("/v1").url()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            server.shutdown()
        }
    }

    @Test
    fun canBeCreated() {
        HTTPClient(baseURL)
    }

    @Test
    fun canPerformASimpleGet() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        HTTPClient(baseURL)
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

        val client = HTTPClient(baseURL)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.responseCode).`as`("responsecode is 223").isEqualTo(223)
    }

    @Test
    fun parsesTheBody() {
        val response = MockResponse().setBody("{'response': 'OK'}").setResponseCode(223)
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        val result = client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        server.takeRequest()

        assertThat(result.body!!.getString("response")).`as`("response is OK").isEqualTo("OK")
    }

    // Errors

    @Test(expected = JSONException::class)
    fun reWrapsBadJSONError() {
        val response = MockResponse().setBody("not uh jason")
        server.enqueue(response)

        val client = HTTPClient(baseURL)
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

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", null as Map<*, *>?, headers)

        val request = server.takeRequest()

        assertThat(request.getHeader("Authentication")).`as`("header is Bearer todd").isEqualTo("Bearer todd")
    }

    @Test
    fun addsDefaultHeadersToRequest() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", null as Map<*, *>?, mapOf("" to ""))

        val request = server.takeRequest()

        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(request.getHeader("X-Platform")).isEqualTo("android")
        assertThat(request.getHeader("X-Platform-Version")).isEqualTo(Integer.toString(android.os.Build.VERSION.SDK_INT))
        assertThat(request.getHeader("X-Version")).isEqualTo(Purchases.frameworkVersion)
    }

    @Test
    fun addsPostBody() {
        val response = MockResponse().setBody("{}")
        server.enqueue(response)

        val body = HashMap<String, String>()
        body["user_id"] = "jerry"

        val client = HTTPClient(baseURL)
        client.performRequest("/resource", body, mapOf("" to ""))

        val request = server.takeRequest()
        assertThat(request.method).`as`("method is POST").isEqualTo("POST")
        assertThat(request.body).`as`("body is not null").isNotNull
        assertThat(request.body.size()).isGreaterThan(0)
    }
}
